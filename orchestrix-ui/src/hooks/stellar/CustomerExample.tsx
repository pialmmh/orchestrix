import React from 'react';
import { observer } from 'mobx-react-lite';
import { useStellarQuery, useStellarMutation } from './StellarQueryHooks';

// Example 1: Simple Customer List
export const CustomerList: React.FC = observer(() => {
  const { data, loading, error, refresh } = useStellarQuery({
    kind: 'customer',
    page: { limit: 10 }
  });

  if (loading) return <div>Loading customers...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h2>Customers</h2>
      <button onClick={refresh}>Refresh</button>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Company</th>
            <th>Contact</th>
            <th>City</th>
          </tr>
        </thead>
        <tbody>
          {data.map((customer: any) => (
            <tr key={customer.CustomerID}>
              <td>{customer.CustomerID}</td>
              <td>{customer.CompanyName}</td>
              <td>{customer.ContactName}</td>
              <td>{customer.City}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
});

// Example 2: Categories with Products (Nested Query)
export const CategoryProducts: React.FC = observer(() => {
  const { data, loading, error } = useStellarQuery({
    kind: 'category',
    page: { limit: 5 },
    include: [{
      kind: 'product',
      page: { limit: 3 }
    }]
  });

  if (loading) return <div>Loading categories...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h2>Categories & Products</h2>
      {data.map((category: any) => (
        <div key={category.CategoryID} style={{ marginBottom: 20 }}>
          <h3>{category.CategoryName}</h3>
          <p>{category.Description}</p>
          <ul>
            {category.products?.slice(0, 3).map((product: any) => (
              <li key={product.ProductID}>
                {product.ProductName} - ${product.UnitPrice}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  );
});

// Example 3: Add New Product Form
export const AddProductForm: React.FC = () => {
  const { mutate, loading, error } = useStellarMutation();
  const [formData, setFormData] = React.useState({
    ProductName: '',
    UnitPrice: '',
    UnitsInStock: '',
    CategoryID: '1'
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      await mutate({
        entityName: 'product',
        operation: 'INSERT',
        data: {
          ...formData,
          UnitPrice: parseFloat(formData.UnitPrice),
          UnitsInStock: parseInt(formData.UnitsInStock),
          CategoryID: parseInt(formData.CategoryID),
          SupplierID: 1 // Default supplier
        }
      });
      
      alert('Product added successfully!');
      // Reset form
      setFormData({
        ProductName: '',
        UnitPrice: '',
        UnitsInStock: '',
        CategoryID: '1'
      });
    } catch (err) {
      console.error('Failed to add product:', err);
    }
  };

  return (
    <div>
      <h2>Add New Product</h2>
      {error && <div style={{ color: 'red' }}>Error: {error}</div>}
      
      <form onSubmit={handleSubmit}>
        <div>
          <label>Product Name:</label>
          <input
            type="text"
            value={formData.ProductName}
            onChange={(e) => setFormData({...formData, ProductName: e.target.value})}
            required
          />
        </div>
        
        <div>
          <label>Price:</label>
          <input
            type="number"
            step="0.01"
            value={formData.UnitPrice}
            onChange={(e) => setFormData({...formData, UnitPrice: e.target.value})}
            required
          />
        </div>
        
        <div>
          <label>Stock:</label>
          <input
            type="number"
            value={formData.UnitsInStock}
            onChange={(e) => setFormData({...formData, UnitsInStock: e.target.value})}
            required
          />
        </div>
        
        <div>
          <label>Category:</label>
          <select
            value={formData.CategoryID}
            onChange={(e) => setFormData({...formData, CategoryID: e.target.value})}
          >
            <option value="1">Beverages</option>
            <option value="2">Condiments</option>
            <option value="3">Confections</option>
            <option value="4">Dairy Products</option>
            <option value="5">Grains/Cereals</option>
            <option value="6">Meat/Poultry</option>
            <option value="7">Produce</option>
            <option value="8">Seafood</option>
          </select>
        </div>
        
        <button type="submit" disabled={loading}>
          {loading ? 'Adding...' : 'Add Product'}
        </button>
      </form>
    </div>
  );
};

// Example 4: Customer Orders with Lazy Loading
export const CustomerOrders: React.FC = observer(() => {
  const [selectedCustomer, setSelectedCustomer] = React.useState<string | null>(null);
  
  // Load customers
  const { data: customers, loading: loadingCustomers } = useStellarQuery({
    kind: 'customer',
    page: { limit: 5 }
  });
  
  // Load orders for selected customer
  const { data: orders, loading: loadingOrders } = useStellarQuery(
    {
      kind: 'salesorder',
      criteria: selectedCustomer ? { CustomerID: [selectedCustomer] } : {},
      page: { limit: 10 },
      include: [{
        kind: 'orderdetail',
        page: { limit: 5 }
      }]
    },
    { autoFetch: !!selectedCustomer }
  );

  if (loadingCustomers) return <div>Loading customers...</div>;

  return (
    <div style={{ display: 'flex', gap: 20 }}>
      <div style={{ flex: 1 }}>
        <h3>Customers</h3>
        {customers.map((customer: any) => (
          <div
            key={customer.CustomerID}
            onClick={() => setSelectedCustomer(customer.CustomerID)}
            style={{
              padding: 10,
              cursor: 'pointer',
              background: selectedCustomer === customer.CustomerID ? '#e0e0e0' : 'white',
              border: '1px solid #ccc',
              marginBottom: 5
            }}
          >
            {customer.CompanyName}
          </div>
        ))}
      </div>
      
      <div style={{ flex: 2 }}>
        <h3>Orders</h3>
        {!selectedCustomer && <p>Select a customer to view orders</p>}
        {loadingOrders && <p>Loading orders...</p>}
        {selectedCustomer && !loadingOrders && (
          <div>
            {orders.length === 0 ? (
              <p>No orders found</p>
            ) : (
              orders.map((order: any) => (
                <div key={order.OrderID} style={{ marginBottom: 15, border: '1px solid #ddd', padding: 10 }}>
                  <strong>Order #{order.OrderID}</strong>
                  <div>Date: {order.OrderDate}</div>
                  <div>Ship to: {order.ShipCity}</div>
                  {order.orderdetails && (
                    <ul>
                      {order.orderdetails.map((detail: any, idx: number) => (
                        <li key={idx}>
                          Product ID: {detail.ProductID}, 
                          Qty: {detail.Quantity}, 
                          Price: ${detail.UnitPrice}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
});

// Main App Component
export const App: React.FC = () => {
  const [activeTab, setActiveTab] = React.useState<'customers' | 'categories' | 'add' | 'orders'>('customers');

  return (
    <div style={{ padding: 20 }}>
      <h1>Stellar Query System - React Example</h1>
      
      <div style={{ marginBottom: 20 }}>
        <button onClick={() => setActiveTab('customers')}>Customers</button>
        <button onClick={() => setActiveTab('categories')}>Categories</button>
        <button onClick={() => setActiveTab('add')}>Add Product</button>
        <button onClick={() => setActiveTab('orders')}>Customer Orders</button>
      </div>
      
      {activeTab === 'customers' && <CustomerList />}
      {activeTab === 'categories' && <CategoryProducts />}
      {activeTab === 'add' && <AddProductForm />}
      {activeTab === 'orders' && <CustomerOrders />}
    </div>
  );
};

export default App;