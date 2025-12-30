# DistroZone-Java

DistroZone is a desktop Point of Sale (POS) and inventory management application designed for clothing stores. Built with Java Swing, it offers a dual-role system for administrators and cashiers, featuring a modern, custom-styled user interface.

## Features

### General
- **Splash Screen:** Professional application loading screen.
- **Secure Login:** User authentication with SHA-256 password hashing.
- **Role-Based Access:** Separate dashboards and functionalities for Admin and Cashier roles.
- **Session Management:** Tracks the currently logged-in user.
- **Operational Hours Validation:** Restricts application access outside of configured business hours.
- **Custom UI:** A modern, rounded-corner UI built with Java Swing, mimicking macOS controls.

### Admin Dashboard
- **User Management:** Create, edit, and delete employee accounts (Admin, Cashier).
- **Master Data Management:** Manage core data categories such as Brands and Product Types.
- **Product Management:** Add and edit core product information.
- **Variant Management:** For each product, manage specific variants including color, size, stock, and status.
- **Photo Management:** Upload and delete product photos for each variant, integrated with Supabase Storage.
- **Reporting:**
    - **Sales Report:** View and export transaction data.
    - **Profit/Loss Report:** Analyze revenue, cost of goods sold (HPP), and profit per product.
- **Operational Hours Configuration:** Set the opening and closing times for the store.

### Cashier Dashboard
- **Point of Sale (POS):** Intuitive interface for processing sales.
- **Product Search:** Quickly find products by name or other attributes.
- **Shopping Cart:** Add items, update quantities, and remove items from the cart.
- **Payment Processing:** Supports multiple payment methods including `cash`, `qris`, and `transfer`.
- **Receipt Generation:** Generates and displays a detailed transaction receipt.
- **Transaction History:** View a history of transactions processed by the logged-in cashier.

## Technology Stack

- **Language:** Java (JDK 17)
- **Framework:** Java Swing for the graphical user interface.
- **Database:** MySQL
- **Cloud Storage:** Supabase Storage for hosting product images.
- **Build System:** Apache Ant (NetBeans project)
- **Key Libraries:**
    - `mysql-connector-j`: For MySQL database connectivity.
    - `okhttp` & `okio`: For handling HTTP requests to Supabase.
    - `jcalendar`: For date chooser components in reports.
    - `Apache POI`: For exporting reports to Excel (`.xlsx`) format.

## Project Structure

The source code is organized into the following packages:

- `config`: Contains database (`DatabaseConfig.java`) and cloud storage connection configurations.
- `model`: Includes POJOs (Plain Old Java Objects) that represent the database tables (e.g., `User.java`, `Product.java`, `Transaction.java`).
- `utils`: A collection of helper classes for common tasks such as:
    - `FormatterUtils`: For currency and date formatting.
    - `InputValidator`: For validating user input like emails, phone numbers, and prices.
    - `SecurityUtils`: For password hashing and verification.
    - `SessionManager`: For managing the logged-in user's session.
    - `SupabaseStorage`: For handling image uploads and deletions.
- `view`: Contains all the GUI classes, including frames, panels, and dialogs that make up the application's user interface.

## Setup and Installation

### Prerequisites
- Java Development Kit (JDK) 17 or later.
- A running MySQL server instance.
- An Apache NetBeans IDE (recommended).

### 1. Database Setup
1.  Create a new database in MySQL named `distro_zone`.
2.  The application is configured to connect to `jdbc:mysql://localhost:3306/distro_zone` with the username `root` and an empty password.
3.  If your MySQL configuration is different, update the connection details in `src/config/DatabaseConfig.java`.
4.  You will need to create the database schema based on the classes in the `src/model` directory.

### 2. Supabase Setup (for Product Images)
1.  Create a project on [Supabase](https://supabase.com/).
2.  Inside your project, go to the **Storage** section and create a new public bucket.
3.  Note your project's **URL** and **service_role key** (found in `Project Settings > API`).
4.  Update the `config/SupabaseConfig.java` file (not present in the tree, but referenced by `SupabaseStorage.java`) with your Supabase URL, service key, and bucket name.

### 3. Running the Application
1.  Clone this repository or download the source code.
2.  Open the project folder in Apache NetBeans.
3.  The required libraries are included in the `lib` folder and referenced in the project properties.
4.  Locate the `view.Main.java` file in the `src` directory.
5.  Right-click `Main.java` and select "Run File" to launch the application. The splash screen will appear, followed by the login form.