package com.orchestrix.stellar.schema;

import com.orchestrix.stellar.model.Kind;
import java.util.List;
import java.util.Map;

public final class NorthwindSchema {
    private NorthwindSchema() {}
    
    public static SchemaMeta northwind() {
        return new SchemaMeta(
            Map.of(
                // Categories and Products
                Kind.CATEGORY, new TableMeta("category", "cat", 
                    List.of("categoryId", "categoryName", "description"), "categoryId"),
                Kind.PRODUCT, new TableMeta("product", "prod", 
                    List.of("productId", "productName", "supplierId", "categoryId", 
                            "quantityPerUnit", "unitPrice", "unitsInStock", 
                            "unitsOnOrder", "reorderLevel", "discontinued"), "productId"),
                
                // Customers and Orders
                Kind.CUSTOMER, new TableMeta("customer", "cust", 
                    List.of("custId", "companyName", "contactName", "contactTitle", 
                            "address", "city", "region", "postalCode", "country", 
                            "phone", "mobile", "email", "fax"), "custId"),
                Kind.SALESORDER, new TableMeta("salesorder", "so", 
                    List.of("orderId", "custId", "employeeId", "orderDate", 
                            "requiredDate", "shippedDate", "shipperid", "freight", 
                            "shipName", "shipAddress", "shipCity", "shipRegion", 
                            "shipPostalCode", "shipCountry"), "orderId"),
                Kind.ORDERDETAIL, new TableMeta("orderdetail", "od", 
                    List.of("orderDetailId", "orderId", "productId", "unitPrice", 
                            "quantity", "discount"), "orderDetailId"),
                
                // Employees and Suppliers
                Kind.EMPLOYEE, new TableMeta("employee", "emp", 
                    List.of("employeeId", "lastname", "firstname", "title", 
                            "titleOfCourtesy", "birthDate", "hireDate", "address", 
                            "city", "region", "postalCode", "country", "phone", 
                            "extension", "mobile", "email", "mgrId"), "employeeId"),
                Kind.SUPPLIER, new TableMeta("supplier", "sup", 
                    List.of("supplierId", "companyName", "contactName", "contactTitle", 
                            "address", "city", "region", "postalCode", "country", 
                            "phone", "email", "fax"), "supplierId"),
                Kind.SHIPPER, new TableMeta("shipper", "ship", 
                    List.of("shipperId", "companyName", "phone"), "shipperId")
            ),
            List.of(
                // Product relationships
                new JoinEdge(Kind.CATEGORY, Kind.PRODUCT, "categoryId", "categoryId"),
                new JoinEdge(Kind.SUPPLIER, Kind.PRODUCT, "supplierId", "supplierId"),
                
                // Order relationships
                new JoinEdge(Kind.CUSTOMER, Kind.SALESORDER, "custId", "custId"),
                new JoinEdge(Kind.EMPLOYEE, Kind.SALESORDER, "employeeId", "employeeId"),
                new JoinEdge(Kind.SHIPPER, Kind.SALESORDER, "shipperId", "shipperid"),
                new JoinEdge(Kind.SALESORDER, Kind.ORDERDETAIL, "orderId", "orderId"),
                
                // OrderDetail to Product relationship
                new JoinEdge(Kind.ORDERDETAIL, Kind.PRODUCT, "productId", "productId"),
                new JoinEdge(Kind.PRODUCT, Kind.ORDERDETAIL, "productId", "productId")
            )
        );
    }
}