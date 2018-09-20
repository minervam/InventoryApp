package com.example.android.inventoryapp.data;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.android.inventoryapp.R;
import com.example.android.inventoryapp.data.BookContract.BookEntry;

/**
 * {@link ContentProvider} for this book inventory app.
 */
public class BookProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = BookProvider.class.getSimpleName();

    /**
     * URI matcher code for the content URI for the pets table
     */
    private static final int BOOKS = 100;

    /**
     * URI matcher code for the content URI for a single pet in the pets table
     */
    private static final int BOOK_ID = 101;

    /**
     * Database Helper Object
     */
    private BookDbHelper mDbHelper;

    /**
     * Column Tags used only for validation purpose
     */
    private static final String TAG_NAME = "title";
    private static final String TAG_PRICE = "price";
    private static final String TAG_QUANTITY = "quantity";
    private static final String TAG_SUPPLIER = "supplierName";
    private static final String TAG_PHONE = "supplierPhone";

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // This URI is used to provide access to MULTIPLE rows
        // of the books table.
        sUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_BOOKS, BOOKS);

        // Maps to the integer code {@link #BOOK_ID}. This URI is used to provide access to a single
        // row on the books table.
        //
        // The "#" wildcard is used where "#" can be substituted for an integer.
        sUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_BOOKS + "/#", BOOK_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new BookDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);

        switch (match) {
            case BOOKS:
                // Query the books table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the books table.
                cursor = database.query(BookEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;

            case BOOK_ID:
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?".
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(BookEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;

            default:
                throw new IllegalArgumentException(getContext().getString(R.string.unknown_uri_exception, uri));
        }

        // This updates the Cursor when the data on this URI changes.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    /**
     * Method to determine type of URI used to query the table
     *
     * @param uri
     * @return
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case BOOKS:
                return BookEntry.CONTENT_LIST_TYPE;

            case BOOK_ID:
                return BookEntry.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    /**
     * Insert a book into the database with the given content values.
     *
     * @param uri
     * @param contentValues
     * @return uri
     */
    @SuppressLint("StringFormatInvalid")
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        long id;
        String columnsToValidate = TAG_NAME + "|" + TAG_PRICE + "|" + TAG_QUANTITY + "|"
                + TAG_SUPPLIER + "|" + TAG_PHONE;

        boolean isValidInput = validateInput(contentValues, columnsToValidate);

        if (isValidInput) {
            SQLiteDatabase writableDatabase = mDbHelper.getWritableDatabase();
            id = writableDatabase.insert(BookEntry.TABLE_NAME, null, contentValues);
        } else {
            id = -1;
        }

        // Check if ID is -1, which means record insert has failed
        if (id == -1) {
            Log.e(LOG_TAG, (getContext().getString(R.string.insert_error, uri)));
            return null;
        }

        //Notify all listeners tha the data has changed
        getContext().getContentResolver().notifyChange(uri, null);

        //Return the new URI with the ID of the newly inserted row appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {

        final int match = sUriMatcher.match(uri);

        switch (match) {
            case BOOKS:
                return updateBook(uri, contentValues, selection, selectionArgs);

            case BOOK_ID:
                // For the BOOK_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateBook(uri, contentValues, selection, selectionArgs);

            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Updates the book inventory in the database with the given content values and returns the
     * number of rows that were successfully updated.
     */
    private int updateBook(Uri uri, ContentValues contentValues, String selection,
                           String[] selectionArgs) {

        String columnsToValidate = null;
        StringBuilder stringBuilder = new StringBuilder();
        final String SEPARATOR = "|";
        int rowsUpdated = 0;

        // If there are no values to update, they don't try to update the database.
        if (contentValues.size() == 0) {
            return 0;
        } else {
            // check that the name value is not null.
            if (contentValues.containsKey(BookEntry.COLUMN_BOOK_NAME)) {
                stringBuilder.append(TAG_NAME);
            } else if (contentValues.containsKey(BookEntry.COLUMN_BOOK_PRICE)) {
                stringBuilder.append(SEPARATOR).append(TAG_PRICE);
            } else if (contentValues.containsKey(BookEntry.COLUMN_BOOK_QUANTITY)) {
                stringBuilder.append(SEPARATOR).append(TAG_QUANTITY);
            } else if (contentValues.containsKey(BookEntry.COLUMN_SUPPLIER_NAME)) {
                stringBuilder.append(SEPARATOR).append(TAG_SUPPLIER);
            } else if (contentValues.containsKey(BookEntry.COLUMN_SUPPLIER_PHONE)) {
                stringBuilder.append(SEPARATOR).append(TAG_PHONE);
            }


            columnsToValidate = stringBuilder.toString();
            boolean isValidInput = validateInput(contentValues, columnsToValidate);

            if (isValidInput) {
                SQLiteDatabase writableDatabase = mDbHelper.getWritableDatabase();

                //Perform the update on the database and get the number of rows affected
                rowsUpdated = writableDatabase.update(BookEntry.TABLE_NAME, contentValues, selection,
                        selectionArgs);

                //If one or more rows were updated, then notify all listeners that the data at the
                //given URI has changed
                if (rowsUpdated != 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
            }
            return rowsUpdated;
        }
    }

    /**
     * This method validates the inputs used in INSERT and UPDATE queries
     *
     * @param values  - ContentValues
     * @param columns
     * @return true/false
     */
    public boolean validateInput(ContentValues values, String columns) {

        String[] columnArgs = columns.split("|");
        String bookName = null;
        Double bookPrice = null;
        Integer bookQuantity = null;
        String supplier = null;
        String supplierPhone = null;

        for (int i = 0; i < columnArgs.length; i++) {

            if (columnArgs[i].equals(TAG_NAME)) {
                //Check if Book name is not null
                bookName = values.getAsString(BookEntry.COLUMN_BOOK_NAME);
                if (bookName == null || bookName.trim().length() == 0) {
                    throw new IllegalArgumentException(getContext().getString(R.string.invalid_name_exception));
                }
            } else if (columnArgs[i].equals(TAG_PRICE)) {
                //Check if Price is provided
                bookPrice = values.getAsDouble(BookEntry.COLUMN_BOOK_PRICE);
                if (bookPrice == null || bookPrice < 0) {
                    throw new IllegalArgumentException(getContext().getString(R.string.invalid_price_exception));
                }
            } else if (columnArgs[i].equals(TAG_QUANTITY)) {
                //Check if quantity is provided
                bookQuantity = values.getAsInteger(BookEntry.COLUMN_BOOK_QUANTITY);
                if (bookQuantity == null || bookQuantity < 0) {
                    throw new IllegalArgumentException(getContext().getString(R.string.invalid_quantity_exception));
                }
            } else if (columnArgs[i].equals(TAG_SUPPLIER)) {
                //Check if supplier name is provided
                supplier = values.getAsString(BookEntry.COLUMN_SUPPLIER_NAME);
                if (supplier == null || supplier.trim().length() == 0) {
                    throw new IllegalArgumentException(getContext().getString(R.string.invalid_supplier_exception));
                }
            } else if (columnArgs[i].equals(TAG_PHONE)) {
                //Check if supplier phone number is provided
                supplierPhone = values.getAsString(BookEntry.COLUMN_SUPPLIER_PHONE);
                if (supplierPhone == null || supplierPhone.trim().length() == 0) {
                    throw new IllegalArgumentException(getContext().getString(R.string.invalid_phone_exception));
                }
            }
        }
        return true;
    }

    /**
     * This method deletes records from the table
     *
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return number of rows deleted
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase writableDatabase = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case BOOKS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = writableDatabase.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                break;

            case BOOK_ID:
                // Delete a single row given by the ID in the URI
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = writableDatabase.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException(getContext().getString(R.string.unknown_uri_exception, uri));
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }
}
