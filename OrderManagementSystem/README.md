# Order Management System (Java JDBC + MySQL)

## Requirements
- JDK 17 or later
- Eclipse IDE with Maven support (Spring Tool Suite also works)
- MySQL Server and MySQL Workbench
- Internet access on the first Maven build to download MySQL Connector/J

## 1. Create the database and table
1. Open MySQL Workbench and connect to your local MySQL server.
2. Open `src/main/resources/schema.sql`.
3. Execute the complete script. It creates `ecommerce_db` and the `Orders` table.

## 2. Set your MySQL password
Open `src/main/java/com/ecommerce/OrderManagementSystem.java` and edit:
```java
private static final String USER = "root";
private static final String PASSWORD = "YOUR_MYSQL_PASSWORD";
```
Replace `YOUR_MYSQL_PASSWORD` with your actual MySQL password. If your root account has no password, use an empty string: `""`.

The default connection URL expects MySQL on `localhost`, port `3306`.

## 3. Import into Eclipse / Spring Tool Suite
1. Extract the ZIP file.
2. Open Eclipse.
3. Select **File > Import > Maven > Existing Maven Projects**.
4. Browse to the extracted `OrderManagementSystem` folder.
5. Select the `pom.xml`, then click **Finish**.
6. If dependencies do not download automatically, right-click the project and choose **Maven > Update Project**, then click **OK**.
7. Expand `src/main/java > com.ecommerce`.
8. Right-click `OrderManagementSystem.java` and choose **Run As > Java Application**.

## 4. Run
The application displays a menu in the Eclipse Console:
- 1: Insert three sample orders (duplicate primary keys are handled)
- 2: Search by status, only where quantity > 2, sorted by unit price descending
- 3: Increase an order's quantity
- 4: Validate and update status, then display the updated order
- 5: Display `SUM(quantity * unit_price)`
- 6: Delete only when the order status is `Cancelled`
- 0: Exit

You can also run from a terminal in the project directory with:
```bash
mvn clean compile
mvn exec:java
```

## Notes
- The sample order IDs are 1001, 1002, and 1003. Running option 1 more than once will report duplicates instead of crashing.
- Valid statuses for operation 4 are `Pending`, `Shipped`, and `Delivered`.
- Option 6 uses a single conditional DELETE statement, so a non-Cancelled order cannot be deleted.
