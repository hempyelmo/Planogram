package course.examples.helloandroid;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.sql.*;

//TODO: Change variable name "position" for "loc" to avoid ambiguity

public class Planogram {

    private int mNbProducts;
    private Product[] mProducts;

    private boolean[] mIsPlaced;
    private boolean[] mIsNew;

    Connection dbConnection = null;

    public Planogram(File pdfFile) {

        int i = 0;

        int shelfNumber = 0;
        int shelfHeight = 0;

        String pdfString = null;

        // Put the PDF in a string
        try {
            PDDocument document = null;
            document = PDDocument.load(pdfFile);
            document.getClass();
            if( !document.isEncrypted() ){
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition( true );
                PDFTextStripper Tstripper = new PDFTextStripper();
                pdfString = Tstripper.getText(document);
                //System.out.println("Text:"+pdfString);

                document.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        // Pattern to match the products lines
        String prodLinePattern = "([0-9]{1,3})\\s([0-9]{6})\\s([0-9]{12})\\s(\\S+)\\s(.*)\\s([0-9]+\\sX\\s[0-9]+\\s\\w+)\\s([0-9])";
        Pattern prodPatternObject = Pattern.compile(prodLinePattern);

        // Count number of products... "There's got to be a better way!"
        Matcher nbProductsMatcher = prodPatternObject.matcher(pdfString);
        mNbProducts = 0;
        while (nbProductsMatcher.find())
            mNbProducts++;

        // Initialize the array for the products
        mProducts = new Product[mNbProducts];

        //Matches date
        String datePattern = "([0-9]{1,2})\\s(\\w{3,})\\s([0-9]{4})";
        Pattern datePatternObject = Pattern.compile(datePattern);
        Matcher dateMatcher = datePatternObject.matcher(pdfString);

        if (dateMatcher.find()) {
    	/*System.out.println("Found value: " + "\"" + dateMatcher.group(0) + "\"" );
		System.out.println("Found value: " + "\"" + dateMatcher.group(1) + "\"" );
		System.out.println("Found value: " + "\"" + dateMatcher.group(2) + "\"" );
		System.out.println("Found value: " + "\"" + dateMatcher.group(3) + "\"" );*/
        }

        String[] lines = pdfString.split(System.getProperty("line.separator"));

        // Main loop to parse the string from the PDF
        for (String currentLine: lines) {

            //Matches shelves number and height
            String shelfPattern = "Tablette ([0-9]{1,2}).*Hauteur = ([0-9]{1,2})";
            Pattern shelfPatternObject = Pattern.compile(shelfPattern);
            Matcher shelfMatcher = shelfPatternObject.matcher(currentLine);

            if (shelfMatcher.find()) {
			/*System.out.println("Found value: " + "\"" + shelfMatcher.group(0) + "\"" );
			System.out.println("Found value: " + "\"" + shelfMatcher.group(1) + "\"" );
			System.out.println("Found value: " + "\"" + shelfMatcher.group(2) + "\"" );*/

                shelfNumber = Integer.parseInt(shelfMatcher.group(1));
                shelfHeight = Integer.parseInt(shelfMatcher.group(2));
            }

            // Matches product
            Matcher prodMatcher = prodPatternObject.matcher(currentLine);
            if (prodMatcher.find()) {
  	    	/*System.out.println("Found value: " + "\"" + prodMatcher.group(0) + "\"" );
			System.out.println("Found value: " + "\"" + prodMatcher.group(1) + "\"" );
			System.out.println("Found value: " + "\"" + prodMatcher.group(2) + "\"" );
			System.out.println("Found value: " + "\"" + prodMatcher.group(3) + "\"" );
			System.out.println("Found value: " + "\"" + prodMatcher.group(4) + "\"" );
			System.out.println("Found value: " + "\"" + prodMatcher.group(5) + "\"" );
			System.out.println("Found value: " + "\"" + prodMatcher.group(6) + "\"" );
			System.out.println("Found value: " + "\"" + prodMatcher.group(7) + "\"" );*/

                mProducts[i] = new Product(prodMatcher.group(2),
                        prodMatcher.group(3),
                        prodMatcher.group(5),
                        prodMatcher.group(6),
                        Integer.parseInt(prodMatcher.group(7)),
                        shelfNumber,shelfHeight,false);

                //System.out.println(i + " " + products[i].getDesc());

                i++;

            }


        }
        // End of main loop

        // Array for indicating if the product has been placed correctly
        mIsPlaced = new boolean[mNbProducts];
        Arrays.fill(mIsPlaced, false);

        // Array for indicating if product is new and not in the inventory
        mIsNew = new boolean[mNbProducts];
        Arrays.fill(mIsNew, false);
    }

    public void saveInDatabase(String dbFilename){

        int i;
        int pos = 0;

        Statement stmt = null;

        System.out.println("bla");

        try {
            Class.forName("org.sqlite.JDBC");
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFilename + ".db");

            stmt = dbConnection.createStatement();

            String sql = "CREATE TABLE PLANOGRAM " +
                    "(POS INT PRIMARY KEY	NOT NULL," +
                    "IDNB	CHAR(6)," +
                    "UPC CHAR(12)	NOT NULL," +
                    "DESCRIPTION	TEXT," +
                    "FORMAT        TEXT," +
                    "NBFACING	INT NOT NULL," +
                    "SHELFNB	INT NOT NULL," +
                    "SHELFHEIGHT INT NOT NULL)";
            stmt.executeUpdate(sql);

            for(i = 0; i < mNbProducts; i++) {
                pos = i + 1;
                sql = "INSERT INTO PLANOGRAM (POS,IDNB,UPC,DESCRIPTION,FORMAT,NBFACING,SHELFNB,SHELFHEIGHT) " +
                        "VALUES (" + pos + ",'" + mProducts[i].getIdNb() + "','" + mProducts[i].getUpc() + "','" +
                        mProducts[i].getDesc() + "','" + mProducts[i].getFormat() + "'," +
                        mProducts[i].getNbFacing() + "," + mProducts[i].getShelfNb() + "," +
                        mProducts[i].getShelfHeight() + ");";
                stmt.executeUpdate(sql);
            }

            stmt.close();
            dbConnection.close();

        } catch(Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

    }

    /**
     * Find product
     *
     * Returns position of the first product found or 0 if it's not found
     *
     **/
    public int findProduct() {

        int prodPosition;

        prodPosition = 0;

        return prodPosition;
    }

    /**
     * Insert product into planogram
     *
     * If product is not found it will be marked as "removed"
     *
     **/
    public void insertProduct() {
    }

    public void show() {
    }

    public void setNewProdAtPos(int position, boolean isNewProd){

        mProducts[position].setIsNewProd(isNewProd);
    }

    public void setExpirationAtPos(int position, Expiration exp){

        mProducts[position].setExpiration(exp);
    }

    public Product getProduct(int position){

        return mProducts[position];
    }

    public int getNbProducts(){

        return mNbProducts;
    }

    public void productIsPlaced(int loc) {
        mIsPlaced[loc] = true;
    }

    public boolean isProductPlaced(int loc) {
        return mIsPlaced[loc];
    }

    public void productIsNew(int loc) {
        mIsNew[loc] = true;
    }

    public boolean isProductNew(int loc) {
        return mIsNew[loc];
    }

}