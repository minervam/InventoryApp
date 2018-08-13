package com.example.android.inventoryapp.data;

import android.provider.BaseColumns;

public final class BookContract {

    private BookContract() {}

    /**
     * Inner class that defines constant values for the books database table.
     * Each entry in the table represents a book.
     */
    public static final class BookEntry implements BaseColumns {

        /** Name of database table for books */
        public final static String TABLE_NAME = "books";

        /**
         * Unique ID number for the book (only for use in the database table).
         *
         * Type: INTEGER
         */
        public final static String _ID = BaseColumns._ID;

        /**
         * Title of the Book.
         *
         * Type: TEXT
         */
        public final static String COLUMN_PRODUCT_NAME ="title";

        /**
         * Price of the Book.
         *
         * Type: TEXT
         */
        public final static String COLUMN_BOOK_PRICE = "price";

        /**
         * Quantity of the Book.
         *
         * Type: INTEGER
         */
        public final static String COLUMN_BOOK_QUANTITY = "quantity";

        /**
         * Name of the Supplier of the Book.
         *
         * Type: TEXT
         */
        public final static String COLUMN_SUPPLIER_NAME ="supplierName";

        /**
         * Phone Number of the Supplier of the Book.
         *
         * Type: TEXT
         */
        public final static String COLUMN_SUPPLIER_PHONE = "supplierPhoneNumber";
    }
}
