import { makeAutoObservable, runInAction } from 'mobx';
import axios from 'axios';

interface QueryNode {
  kind: string;
  criteria?: Record<string, any>;
  page?: { limit: number; offset?: number };
  include?: QueryNode[];
  lazy?: boolean;
  lazyLoadKey?: string;
}

interface LazyPlaceholder {
  key: string;
  kind: string;
  path: string;
  loadUrl: string;
}

interface QueryResult {
  data: any[];
  lazyLoaders?: Record<string, LazyPlaceholder>;
}

export class LazyQueryObservable {
  private baseUrl: string;
  private _data: any[] = [];
  private _loading: boolean = false;
  private _error: string | null = null;
  private _lazyLoaders: Map<string, LazyLoader> = new Map();
  private _query: QueryNode;

  constructor(query: QueryNode, baseUrl: string = 'http://localhost:8090') {
    this._query = query;
    this.baseUrl = baseUrl;
    makeAutoObservable(this);
  }

  get data() {
    return this._data;
  }

  get loading() {
    return this._loading;
  }

  get error() {
    return this._error;
  }

  get lazyLoaders() {
    return Array.from(this._lazyLoaders.values());
  }

  async execute() {
    runInAction(() => {
      this._loading = true;
      this._error = null;
    });

    try {
      const response = await axios.post(`${this.baseUrl}/api/lazy/query`, this._query);
      const result: QueryResult = response.data;

      runInAction(() => {
        this._data = result.data;
        this._loading = false;

        // Create lazy loaders for each lazy placeholder
        if (result.lazyLoaders) {
          Object.entries(result.lazyLoaders).forEach(([path, placeholder]) => {
            const loader = new LazyLoader(placeholder, this.baseUrl);
            this._lazyLoaders.set(path, loader);
          });
        }
      });

      // Merge lazy loaded data into main results
      this.mergeLazyData();

    } catch (error: any) {
      runInAction(() => {
        this._error = error.message;
        this._loading = false;
      });
    }
  }

  private mergeLazyData() {
    // This method would merge lazy loaded data into the main data structure
    // Implementation depends on how you want to structure the nested data
  }

  getLazyLoader(path: string): LazyLoader | undefined {
    return this._lazyLoaders.get(path);
  }

  async loadLazy(path: string, parentIds?: Record<string, any>) {
    const loader = this._lazyLoaders.get(path);
    if (loader) {
      await loader.load(parentIds);
      this.mergeLazyData();
    }
  }
}

export class LazyLoader {
  private placeholder: LazyPlaceholder;
  private baseUrl: string;
  private _data: any[] = [];
  private _loading: boolean = false;
  private _loaded: boolean = false;
  private _error: string | null = null;

  constructor(placeholder: LazyPlaceholder, baseUrl: string) {
    this.placeholder = placeholder;
    this.baseUrl = baseUrl;
    makeAutoObservable(this);
  }

  get data() {
    return this._data;
  }

  get loading() {
    return this._loading;
  }

  get loaded() {
    return this._loaded;
  }

  get error() {
    return this._error;
  }

  get key() {
    return this.placeholder.key;
  }

  get kind() {
    return this.placeholder.kind;
  }

  get path() {
    return this.placeholder.path;
  }

  async load(parentIds?: Record<string, any>) {
    if (this._loaded && !parentIds) {
      return; // Already loaded
    }

    runInAction(() => {
      this._loading = true;
      this._error = null;
    });

    try {
      const response = await axios.post(
        `${this.baseUrl}/api/lazy/load/${this.placeholder.key}`,
        parentIds || {}
      );

      runInAction(() => {
        this._data = response.data.data;
        this._loading = false;
        this._loaded = true;
      });
    } catch (error: any) {
      runInAction(() => {
        this._error = error.message;
        this._loading = false;
      });
    }
  }
}

// Helper function to create and execute a lazy query
export function createLazyQuery(query: QueryNode, baseUrl?: string): LazyQueryObservable {
  const observable = new LazyQueryObservable(query, baseUrl);
  observable.execute(); // Auto-execute on creation
  return observable;
}

// Example usage:
/*
const lazyQuery = {
  kind: "customer",
  criteria: { CustomerID: ["ALFKI", "ANATR"] },
  page: { limit: 20 },
  include: [{
    kind: "salesorder",
    lazy: true,
    page: { limit: 10 },
    include: [{ 
      kind: "orderdetail", 
      page: { limit: 5 } 
    }]
  }]
};

const queryObservable = createLazyQuery(lazyQuery);

// React component example:
import { observer } from 'mobx-react-lite';

const CustomerList = observer(() => {
  const [query] = useState(() => createLazyQuery(lazyQuery));
  
  if (query.loading) return <div>Loading...</div>;
  if (query.error) return <div>Error: {query.error}</div>;
  
  return (
    <div>
      {query.data.map(customer => (
        <div key={customer.CustomerID}>
          <h3>{customer.CompanyName}</h3>
          <button onClick={() => query.loadLazy('customer.salesorder', { CustomerID: customer.CustomerID })}>
            Load Orders
          </button>
          {query.getLazyLoader('customer.salesorder')?.loading && <span>Loading orders...</span>}
          {query.getLazyLoader('customer.salesorder')?.data.map(order => (
            <div key={order.OrderID}>Order #{order.OrderID}</div>
          ))}
        </div>
      ))}
    </div>
  );
});
*/