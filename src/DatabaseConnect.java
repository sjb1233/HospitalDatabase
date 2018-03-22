import java.sql.*;

public class DatabaseConnect {
    private static String username = "cs421g20";
    private static String password = "pg20cs421";

    public Connection conn = null;

    public DatabaseConnect() throws Exception{
       try{
           DriverManager.registerDriver ( new org.postgresql.Driver() ) ;
       }
        catch(Exception e){
           System.out.println("Can't find the Class ::: DatabaseConnect()");
           throw new Exception();
        }

        try{
            this.conn = DriverManager.getConnection(
                    "jdbc:postgresql://comp421.cs.mcgill.ca:5432/cs421", username, password);
        }catch (SQLException e){
            System.out.println("Error DriverManager.getConnection ::: DatabaseConnect()");
            throw new Exception();
        }

        if (this.conn == null){
            System.out.println("Failed to make connection - conn == null ::: DatabaseConnect()");
        }
    }



}
