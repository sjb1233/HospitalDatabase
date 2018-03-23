import java.util.Scanner;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;

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
                        nurseRounds(conn);
                        break;
                    case "c":
                        diagnoseAndPrescribe(conn);
                        break;
                    case "d":
                        chargePatient(conn);
                        break;
                    case "e":
                        releasePatient(conn);
                    case "f":
//                        Quarantine()
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

        String patientName = "'" + getUserInput("Full name of patient being admitted (First Last)", "(\\w)+\\s(\\w)+") + "'";

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
            String name = "'" + getUserInput("Enter full name of patient","(\\w){1,20}\\s(\\w){1,20}") + "'";
            String sex = "'" + getUserInput("Enter Sex (M/F)", "\\w") + "'";
            String dateOfBirth = "'" + getUserInput("Enter date of birth (YYYY-MM-DD)", "\\d{4}-\\d{2}-\\d{2}") + "'";
            String history = "'" + getUserInput("Enter patient medical history", "(\\w|\\s){1,5000}") + "'";
            String address = "'" + getUserInput("Enter patient address", "(\\w|\\s){1,200}") + "'";
            String contactNumber = "'" + getUserInput("Enter patient contact number ###-###-####", "\\d{3}-\\d{3}-\\d{4}") + "'";
            String emergencyContact = "'" + getUserInput("Enter patient emergency contact", "(\\w){1,20}\\s(\\w){1,20}") + "'";
            String emergencyContactNumber = "'" + getUserInput("Enter patient emergency contact's number ###-###-####", "\\d{3}-\\d{3}-\\d{4}") + "'";
            String insert = "INSERT INTO Patient(name, sex, dateOfBirth, history, address, contactNumber, emergencyContact, emergencyContactNumber)" + " VALUES (" + name + "," + sex + "," + dateOfBirth + "," + history + "," + address + "," + contactNumber + "," + emergencyContact + "," + emergencyContactNumber + ")";

            stmt.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
            rs = stmt.getGeneratedKeys();
            if(rs.next())
                pid = rs.getInt("pid");
            else{
                System.out.println("Something went wrong");
                return;
            }
        }

//        Check that patient isn't already admitted
        String admitted = "SELECT * FROM Patient,Occupies,Occupy WHERE releasedDate IS NULL " +
                "AND admissionDate IS NOT NULL AND " +
                "Occupies.occupyID = Occupy.occupyID AND Patient.pid = Occupies.pid AND Patient.pid = " + pid;
        rs = stmt.executeQuery(admitted);
        if(rs.next()) {
            System.out.println("Patient already admitted to hospital");
            return;
        }


//       Date = YYYY-MM-DD
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String currDate = "'" + dateFormat.format(date) + "'";

        String desc = "'" + getUserInput("Why is the patient being admitted today","(\\w|\\s){1,5000}") + "'";
        String record = "INSERT INTO Record(pID, eID, reportDate, description) VALUES (" + pid + "," + eid + "," + currDate + "," + desc + ")";

        stmt.executeUpdate(record, Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        int rid;
        if(rs.next())
            rid = rs.getInt("rid");
        else{
            System.out.println("Something went wrong");
            return;
        }

        String maintain = "INSERT INTO Maintains VALUES (" + eid + "," + rid + ")";
        stmt.executeUpdate(maintain, Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        if(!rs.next()) {
            System.out.println("Something went wrong");
            return;
        }

        String hospital = getUserInput("Enter hospital (Mount Royal General Hospital|Montreal General Hospital|Sherbrooke Medical Facility)", "(Mount Royal General Hospital|Montreal General Hospital|Sherbrooke Medical Facility)");
        String freeRoom = "SELECT Room.roomNumber FROM Room WHERE Room.capacity > Room.numOccupants AND Room.quarantine = 0 AND Room.name = '" + hospital + "'";

        rs = stmt.executeQuery(freeRoom);
        String room;
        if(rs.next()) {
            room = rs.getString("roomNumber");
            System.out.println("\nFree Room: " + room);
        }
        else {
            System.out.println("No free room; cannot admit patient at this time");
            return;
        }

        String occupy = "INSERT INTO Occupy(admissionDate) VALUES (" + currDate + ");";
        stmt.executeUpdate(occupy, Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        int occupyid;
        if(rs.next())
            occupyid = rs.getInt("occupyid");
        else{
            System.out.println("Something went wrong");
            return;
        }

        String occupies = "INSERT INTO Occupies VALUES ('" + hospital + "','" + room + "'," + pid + "," + eid + "," + occupyid + ")";
        stmt.executeUpdate(occupies, Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        if(!rs.next()) {
            System.out.println("Something went wrong");
            return;
        }

        String update = "Update Room SET numoccupants = numoccupants + 1 WHERE name = '" + hospital + "' AND roomnumber = '" + room + "'";
        stmt.executeUpdate(update, Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        if(!rs.next()) {
            System.out.println("Something went wrong");
            return;
        }

        System.out.println("Success");
        rs.close();
        stmt.close();
    }

    private static void nurseRounds(Connection conn) throws SQLException{
        String eid = getUserInput("Please enter your nurse employee id","(\\d)+");

        String query = "SELECT name FROM employee, nurse WHERE employee.eid = nurse.eid AND nurse.eid = " + eid;

        Statement stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery(query);
        if(rs.next())
            System.out.println("\nNurse member: " + rs.getString("name"));
        else {
            System.out.println("No nurse with eid: " + eid);
            return;
        }

        String idList = getUserInput("Enter comma separated list of patients (by id) attended to in the round", "(\\d)+(,(\\d)+)*");
        String[] ids = idList.split(",");

        for(String id : ids){
            String admitted = "SELECT * FROM Patient,Occupies,Occupy WHERE releasedDate IS NULL " +
                    "AND admissionDate IS NOT NULL AND " +
                    "Occupies.occupyID = Occupy.occupyID AND Patient.pid = Occupies.pid AND Patient.pid = " + id;
            rs = stmt.executeQuery(admitted);
            if(!rs.next()) {
                System.out.println("Patient with pid = " + id + " is not currently admitted to the hospital");
                continue;
            }

            // Get profile of patient:
            String latestRecordDate;
            String latestRecord;

            String record = "WITH latestRecord AS (SELECT MAX(reportDate) FROM Record WHERE pid = " + id + ")" +
                    "SELECT Record.rid,Record.pid,latestRecord.max,Record.description FROM Record,latestRecord WHERE Record.reportDate = latestRecord.max" +
                    " AND Record.pid = " + id + " ORDER BY Record.rid DESC";

            rs = stmt.executeQuery(record);
            if(rs.next()) {
                latestRecordDate = rs.getDate("max").toString();
                latestRecord = rs.getString("description");
            }
            else {
                latestRecordDate = "N/A";
                latestRecord = "None";
            }

            String latestStatusDate;
            String latestStatus;

            String status = "WITH latestStatus AS (SELECT MAX(dateIssued) FROM Attend, AttendedBy WHERE pid = " + id + " AND Attend.attendID = AttendedBy.attendID)" +
                    " SELECT Attend.attendID,status,latestStatus.max FROM Attend,AttendedBy,latestStatus WHERE Attend.dateIssued = latestStatus.max AND Attend.attendID = AttendedBy.attendID AND pid = " + id
                    + " ORDER BY Attend.attendID DESC";

            rs = stmt.executeQuery(status);
            if(rs.next()) {
                latestStatusDate = rs.getDate("max").toString();
                latestStatus = rs.getString("status");
            }
            else {
                latestStatusDate = "N/A";
                latestStatus = "None";
            }

            System.out.println("Profile for patient " + id + "\n");

            System.out.println("Latest Record: " + latestRecordDate);
            System.out.println(("Latest Record Description: " + latestRecord + "\n"));

            System.out.println("Latest Status: " + latestStatusDate);
            System.out.println(("Latest Status: " + latestStatus + "\n"));

            String newStatus = "'" + getUserInput("Update status", "(\\w|\\s){1,3000}") + "'";

            //  Date = YYYY-MM-DD
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            String currDate = "'" + dateFormat.format(date) + "'";

            String attend = "INSERT INTO Attend(dateIssued, status) VALUES (" + currDate + "," +  newStatus + ")";
            stmt.executeUpdate(attend, Statement.RETURN_GENERATED_KEYS);
            rs = stmt.getGeneratedKeys();
            int attendId;
            if(rs.next())
                attendId = rs.getInt("attendid");
            else{
                System.out.println("Something went wrong");
                return;
            }

            String attendBy = "INSERT INTO AttendedBy VALUES (" + id + "," + eid +  "," +  attendId + ")";
            stmt.executeUpdate(attendBy, Statement.RETURN_GENERATED_KEYS);
            rs = stmt.getGeneratedKeys();
            if(!rs.next()) {
                System.out.println("Something went wrong");
                return;
            }
        }

        System.out.println("Success");
        rs.close();
        stmt.close();

    }

    private static void diagnoseAndPrescribe(Connection conn) throws SQLException{
        String eid = getUserInput("Please enter your doctor employee id","(\\d)+");

        String query = "SELECT name FROM employee, doctor WHERE employee.eid = doctor.eid AND doctor.eid = " + eid;

        Statement stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery(query);
        if(rs.next())
            System.out.println("\nDoctor member: " + rs.getString("name"));
        else {
            System.out.println("No doctor with eid: " + eid);
            return;
        }

        String id = getUserInput("Enter patient id for patient of interest", "(\\d)+");

        String admitted = "SELECT * FROM Patient,Occupies,Occupy WHERE releasedDate IS NULL " +
                "AND admissionDate IS NOT NULL AND " +
                "Occupies.occupyID = Occupy.occupyID AND Patient.pid = Occupies.pid AND Patient.pid = " + id;
        rs = stmt.executeQuery(admitted);
        if(!rs.next()) {
            System.out.println("Patient with pid = " + id + " is not currently admitted to the hospital");
            return;
        }

        // Get profile of patient:
        String latestRecordDate;
        String latestRecord;

        String record = "WITH latestRecord AS (SELECT MAX(reportDate) FROM Record WHERE pid = " + id + ")" +
                " SELECT Record.rid,Record.pid,latestRecord.max,Record.description FROM Record,latestRecord WHERE Record.reportDate = latestRecord.max" +
                " AND Record.pid = " + id + " ORDER BY Record.rid DESC";

        rs = stmt.executeQuery(record);
        if(rs.next()) {
            latestRecordDate = rs.getDate("max").toString();
            latestRecord = rs.getString("description");
        }
        else {
            latestRecordDate = "Not Applicable";
            latestRecord = "None";
        }

        String latestDoctorDate;
        String latestDiagnosis;
        String latestTreatment;

        String status = "WITH latestTreatment AS (SELECT MAX(dateIssued) FROM treatedBy, Treatment WHERE pid = " + id + " AND treatedBy.treatmentID = Treatment.treatmentID)" +
                " SELECT treatment.treatmentid,diagnosis,treatmentplan,latestTreatment.max FROM treatment,treatedby,latestTreatment WHERE treatment.dateIssued = latestTreatment.max AND " +
                "treatment.treatmentid = treatedby.treatmentid AND pid = " + id + " ORDER BY treatment.treatmentid DESC";

        rs = stmt.executeQuery(status);
        if(rs.next()) {
            latestDoctorDate = rs.getDate("max").toString();
            latestDiagnosis = rs.getString("diagnosis");
            latestTreatment = rs.getString("treatmentplan");
        }
        else {
            latestDoctorDate = "N/A";
            latestDiagnosis = "None";
            latestTreatment = "None";
        }

        System.out.println("Profile for patient " + id + "\n");

        System.out.println("Latest Record: " + latestRecordDate);
        System.out.println(("Latest Record Description: " + latestRecord + "\n"));

        System.out.println("Latest update: " + latestDoctorDate);
        System.out.println(("Latest Diagnosis: " + latestDiagnosis  + "\n"));
        System.out.println(("Latest Treatment: " + latestTreatment + "\n"));

        String newDiagnosis = "'" + getUserInput("Update diagnosis", "(\\w|\\s){1,3000}") + "'";
        String newTreatment = "'" + getUserInput("Update treatment plan", "(\\w|\\s){1,3000}") + "'";

        //  Date = YYYY-MM-DD
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String currDate = "'" + dateFormat.format(date) + "'";

        String treatment = "INSERT INTO Treatment(dateIssued, diagnosis, treatmentPlan) VALUES (" + currDate + "," + newDiagnosis + "," + newTreatment + ")";
        stmt.executeUpdate(treatment, Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        int treatmentid;
        if(rs.next())
            treatmentid = rs.getInt("treatmentid");
        else{
            System.out.println("Something went wrong");
            return;
        }

        String treatedBy = "INSERT INTO treatedby VALUES (" + id + "," + eid +  "," +  treatmentid + ")";
        stmt.executeUpdate(treatedBy, Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        if(!rs.next()) {
            System.out.println("Something went wrong");
            return;
        }

        System.out.println("Success");
        rs.close();
        stmt.close();

    }

    private static void chargePatient(Connection conn) throws SQLException{
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

        String patientName = "'" + getUserInput("Full name of patient being admitted (First Last)", "(\\w)+\\s(\\w)+") + "'";

        String patientQuery = "SELECT pid,name,dateOfBirth,contactnumber FROM patient WHERE name = " + patientName;

        rs = stmt.executeQuery(patientQuery);
        int pid;
        if(rs.next()){
            pid = rs.getInt("pid");
        }else{
            System.out.println(("Patient with name provided not in the database"));
            return;
        }

        String hospital = "'" + getUserInput("Enter hospital (Mount Royal General Hospital|Montreal General Hospital|Sherbrooke Medical Facility)",
                "(Mount Royal General Hospital|Montreal General Hospital|Sherbrooke Medical Facility)") + "'";

        String insuranceCompany = "'" + getUserInput("Enter name of insurance company (Zoombeat|Quatz|Trudoo|Lajo)",
                "(Zoombeat|Quatz|Trudoo|Lajo)") + " Insurance Company'" ;

        String insuranceQuery = "SELECT iid FROM insuranceCompany WHERE name = " + insuranceCompany;

        rs = stmt.executeQuery(insuranceQuery);
        int iid;
        if(rs.next()){
            iid = rs.getInt("iid");
        }else{
            System.out.println(("No insurance company named: " + insuranceCompany));
            return;
        }

        String amount = getUserInput("Enter amount owed", "(\\d)+");
        String desc = "'" + getUserInput("Description", "(\\w|\\s){1,3000}") + "'";

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String currDate = "'" + dateFormat.format(date) + "'";

        String payment = "INSERT INTO Payment(amount, description, dateIssued) VALUES (" +
                amount + "," + desc + "," + currDate + ")";

        stmt.executeUpdate(payment, Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        int paymentid;
        if(rs.next())
            paymentid = rs.getInt("paymentid");
        else{
            System.out.println("Something went wrong");
            return;
        }

        String pays = "INSERT INTO Pays VALUES (" + hospital +"," + pid + "," + iid + "," + paymentid + ")";
        stmt.executeUpdate(pays, Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        if(!rs.next()) {
            System.out.println("Something went wrong");
            return;
        }

        System.out.println("Success");
        rs.close();
        stmt.close();

    }

    private static void releasePatient(Connection conn) throws SQLException{
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

        String patientName = "'" + getUserInput("Full name of patient being released (First Last)", "(\\w)+\\s(\\w)+") + "'";

        String patientQuery = "SELECT pid,name,dateOfBirth,contactnumber FROM patient WHERE name = " + patientName;

        rs = stmt.executeQuery(patientQuery);
        int pid;
        if(rs.next()){
            pid = rs.getInt("pid");
        }else{
            System.out.println(("Patient with name provided not in the database"));
            return;
        }

        String admitted = "SELECT Occupy.occupyID,Occupies.name,Occupies.roomnumber FROM Patient,Occupies,Occupy WHERE releasedDate IS NULL " +
                "AND admissionDate IS NOT NULL AND " +
                "Occupies.occupyID = Occupy.occupyID AND Patient.pid = Occupies.pid AND Patient.pid = " + pid;
        rs = stmt.executeQuery(admitted);
        int occupyid;
        String hospital;
        String roomnumber;
        if(rs.next()) {
            hospital = "'" + rs.getString("name") + "'";
            occupyid = rs.getInt("occupyID");
            roomnumber = "'" + rs.getString("roomnumber") + "'";
        }
        else{
            System.out.println("Patient with pid = " + pid + " is not currently admitted to the hospital");
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String currDate = "'" + dateFormat.format(date) + "'";

        String update = "UPDATE Occupy SET releaseddate = " + currDate + " WHERE occupyid = " + occupyid;
        int number = stmt.executeUpdate(update);
        if(number == 0){
            System.out.println("Couldn't update Occupy table");
            return;
        }

        String room = "SELECT * FROM Room WHERE name = " + hospital + " AND roomNumber = " + roomnumber;
        rs = stmt.executeQuery(room);
        int numoccupants;
        int quarantine;
        if(rs.next()){
            numoccupants = rs.getInt("numoccupants");
            quarantine = rs.getInt("quarantine");
        }
        else{
            System.out.println("Error querying Room table");
            return;
        }

        numoccupants--;
        if(quarantine > 0) quarantine--;

        String updateRoom = "UPDATE Room SET numoccupants = " + numoccupants + ", quarantine = " + quarantine +
                " WHERE name = " + hospital + " AND roomNumber = " + roomnumber;

        number = stmt.executeUpdate(updateRoom);
        if(number == 0){
            System.out.println("Couldn't update Room table");
            return;
        }

        System.out.println("Success");
        rs.close();
        stmt.close();

    }

}
