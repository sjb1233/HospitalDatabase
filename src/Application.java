import java.util.Scanner;
import java.sql.*;

public class Application {

    private static Scanner scan = new Scanner(System.in);

    public static void main(String[] args){
        Connection conn = null;
        try{
            conn = (new DatabaseConnect()).conn;

            System.out.println("Welcome to the Montreal Hospital Network Database Interface\n");
            String input = null;

            do{
                printMainUI();
                input = getUserInput("Enter the option letter", "[abcdeq]");

                switch(input){
                    case "a":
                        admitPatient(conn);
                        break;
                    case "b":
//                        nurseRounds()
                        break;
                    case "c":
//                        diagnoseAndPrescribe()
                        break;
                    case "d":
//                        chargePatient()
                        break;
                    case "e":
//                        releasePatient()
                        break;
                    case "q":
                        System.out.println("Have a great day");
                        break;
                }



            } while(!input.equals("q"));


        }catch(Exception e){
            e.printStackTrace();
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.out.println("Can't close connection");
                e.printStackTrace();
            }
        }
        scan.close();

    }

    private static String getUserInput(String message, String pattern){
        System.out.println();
        String input = null;

        do{
            System.out.print(message + ": ");
            String in = scan.nextLine();
            if(in.matches(pattern))
                input = in;
        }while(input == null);


        return input;
    }

    private static void printMainUI(){
        System.out.println();
        System.out.println("Select from the following options:");
        System.out.println("a) Admit patient to the hospital");
        System.out.println("b) Conduct ward rounds (Nurses)");
        System.out.println("c) Diagnose and prescribe a treatment (Doctors)");
        System.out.println("d) Charge the patient");
        System.out.println("e) Release patient from the hospital");
        System.out.println("q) Exit from the database");

    }

    private static void admitPatient(Connection conn) throws SQLException{
        String eid = getUserInput("Please enter your staff employee id","(\\d)+");

        String query = "SELECT name FROM employee, staff WHERE employee.eid = staff.eid AND staff.eid = " + eid;

        Statement stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery(query);
        if(rs.next())
            System.out.println("\nStaff member: " + rs.getString("name"));
        else {
            System.out.println("No staff with eid: " + eid);
            return;
        }

        String patientName = getUserInput("Full name of patient being admitted", "(\\w)+");

        String patientQuery = "SELECT pid,name,dateOfBirth,contactnumber FROM patient WHERE name = " + patientName;

        rs = stmt.executeQuery(patientQuery);
        int pid;
        if(rs.next()){
            pid = rs.getInt("pid");
            System.out.println("Patient:\npid   " + pid + "\nname   " + rs.getString("name")
            + "\ndateofBirth    " + rs.getDate("dateOfBirth") + "\ncontact  " + rs.getString("contactnumber"));
        }
        else{
            System.out.println("New Patient - Enter their information");
            String name = "'" + getUserInput("Enter full name of patient","(\\w){1,50}") + "'";
            String sex = "'" + getUserInput("Enter Sex: M/F", "\\w") + "'";
            String dateOfBirth = "'" + getUserInput("Enter date of birth (YYYY-MM-DD)", "\\d{4}-\\d{2}-\\d{2}") + "'";
            String history = "'" + getUserInput("Enter patient medical history", "(\\w){1,5000}") + "'";
            String address = "'" + getUserInput("Enter patient address", "\\w{1,200}") + "'";
            String contactNumber = "'" + getUserInput("Enter patient contact number ###-###-####", "\\d{3}-\\d{3}\\d{4}") + "'";
            String emergencyContact = "'" + getUserInput("Enter patient emergency contact", "\\w{1,50}") + "'";
            String emergencyContactNumber = "'" + getUserInput("Enter patient emergency contact's number ###-###-####", "\\d{3}-\\d{3}\\d{4}") + "'";
            String insert = "INSERT INTO Patient(name, sex, dateOfBirth, history, address, contactNumber, emergencyContact, emergencyContactNumber" +
                    " VALUES (" + name + "," + sex + "," + dateOfBirth + "," + history + "," + address + "," + contactNumber + "," + emergencyContact
                    + "," + emergencyContact  + "," + emergencyContactNumber + ")";

            stmt.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
            rs = stmt.getGeneratedKeys();
            if(rs.next())
                pid = rs.getInt("pid");
            else{
                System.out.println("Something went wrong");
                return;
            }

        }

        //create new record + add maintains
        //assign patient to open room
        //update capacity


        rs.close();
        stmt.close();
    }

    private static void nurseRounds(Connection conn) throws SQLException{
        /*
        get EID check that it is a nurse
        fill in patient ids for the round (1,2,3,4,5,6,7,6,5,4,3)
        Check that each are a patient currently in the hospital
        iterate through - get profile (latest record, status, diagnosis, treatment + date of each)
        add -> attend (date, status) && attendedBy
         */
    }

    private static void diagnoseAndPrescribe(Connection conn) throws SQLException{
        /*
        get EID check that it is a doctor
        fill in patient id of iterest
        Check that patient of hospital
        get profile (latest record, status, diagnosis, treatment + date of each)
        add -> treatment + treated by
         */
    }

    private static void chargePatient(Connection conn) throws SQLException{
        /*
        get EID check that it is a staff member
        get pid from name of patient
        Check that patient of hospital
        get IID by name
        add -> payment, pays
         */
    }

    private static void releasePatient(Connection conn) throws SQLException{
        /*
        get EID check that it is a staff member
        get pid from name of patient
        Check that patient of hospital
        add released date; update capacity
         */
    }

}
