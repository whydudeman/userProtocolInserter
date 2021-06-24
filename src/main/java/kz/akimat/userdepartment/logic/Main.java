package kz.akimat.userdepartment.logic;

import kz.akimat.userdepartment.util.DbConstants;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class Main {

    public static void main(String... strings) throws IOException, SQLException {
//        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//        System.out.println(passwordEncoder.encode("Sm82G3As"));
//        String passwords = "zNtug4kB,t5CZFm8v,HzFL6UGY,2GMgzQdx,d4qJ6Kj9,hvrA43Hk";
//        List<String> passwordList = Arrays.asList(passwords.split(","));
//        for (String s: passwordList){
//            System.out.println(passwordEncoder.encode(s));
//        }
        Main objExcelFile = new Main();
        String fileName = "mcriap.xlsx";
        String path = "/home/nurbol/akimat/claim_case/";
        Workbook workbook = getExcelDocument(fileName, path);
        objExcelFile.processExcelObject(workbook);
    }

    private static Workbook getExcelDocument(String fileName, String path) throws IOException {

        File file = new File(path + fileName);

        FileInputStream inputStream = new FileInputStream(file);
        Workbook workbook = null;
        String fileExtensionName = fileName.substring(fileName.indexOf("."));
        if (fileExtensionName.equals(".xlsx")) {
            workbook = new XSSFWorkbook(inputStream);
        } else if (fileExtensionName.equals(".xls")) {
            workbook = new HSSFWorkbook(inputStream);
        }
        return workbook;
    }

    public void processExcelObject(Workbook workbook) throws SQLException {
        for (int i = 0; i < Objects.requireNonNull(workbook).getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            int rowCount = sheet.getLastRowNum();
            List<UserAndDuplicate> sameUsers = new ArrayList<>();
            System.out.println(rowCount);
            for (int j = 1; j <= 58; j++) {
                Row row = sheet.getRow(j);
                insertAndUpdateTask(row, sameUsers);
                System.out.println(j);
            }
            for (UserAndDuplicate userId : sameUsers) {
                System.out.println(userId.toString());
            }
        }
    }

    private void insertAndUpdateTask(Row row, List<UserAndDuplicate> sameUsers) {

        ExcellData excellData = new ExcellData(row);
        System.out.println(excellData.toString());
        Long userId = findUserIdByLogin(excellData.username);
        if (userId == null) {
            Long departmentId = null;
            if (excellData.department != null) {
                departmentId = getDepartmentIfExists(excellData.department);
                if (departmentId == null)
                    departmentId = insertDepartment(excellData.department, excellData.abbreviation);
            }
            Long positionId = insertPosition(excellData.id, excellData.position, departmentId, excellData.parentId);
            boolean active = true;
            Long groupingId = getGroupIfExists(excellData.grouping);
            userId = insertUser(excellData.id, excellData.name, excellData.username, excellData.password, groupingId, positionId, active);
            List<Long> roleIds = new ArrayList<>();
            for (String roleName : excellData.roles) {
                Long roleId = selectRoleByName(roleName.trim());
                roleIds.add(roleId);
            }
            for (Long roleId : roleIds) {
                insertRoleUser(userId, roleId);
            }
//            if (excellData.firstGroup)
//                createExecutionGroupUsers(userId, 2L);
//            if (excellData.secondGroup)
//                createExecutionGroupUsers(userId, 3L);
//            if (excellData.thirdGroup)
//                createExecutionGroupUsers(userId, 4L);
//            if (excellData.forthGroup)
//                createExecutionGroupUsers(userId, 5L);
        } else {
            System.out.println("ERROR: " + userId);
            sameUsers.add(new UserAndDuplicate(excellData.id, userId));


        }


    }

    private void createExecutionGroupUsers(Long userId, Long groupId) {
        String SQL_INSERT = "INSERT INTO `execution_group_users`(`users_id`, `execution_group_id`) VALUES (?,?)";
        try (
                Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                PreparedStatement statement = connection.prepareStatement(SQL_INSERT);
        ) {
            statement.setLong(1, userId);
            statement.setLong(2, groupId);
            statement.executeUpdate();
            // ...
        } catch (SQLException e) {
            System.out.println("ERROR: CONTROL: TASK AND USER ALREADY EXISTS");
        }

    }

    private Long createExecutionGroup(String name) {
        String SQL_INSERT = "INSERT INTO `execution_group`(created_at,`updated_at`,name) VALUES (NOW(),NOW(),?)";
        try (
                Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                PreparedStatement statement = connection.prepareStatement(SQL_INSERT);
        ) {
            statement.setString(1, name);

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
            // ...
        } catch (SQLException e) {
            System.out.println("ExecutionGroup: ExecutionGroup ALREADY EXISTS");
        }
        throw new RuntimeException("Creating execution failed, no ID obtained.");
    }

    private Long getExecutionGroupIfExists(String name) {
        String SQL_SELECT = "SELECT id from execution_group where name=?";
        try (Connection conn = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
            preparedStatement.setString(1, name);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Long getGroupIfExists(String name) {
        String SQL_SELECT = "SELECT id from grouping where name=?";
        try (Connection conn = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
            preparedStatement.setString(1, name);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Long getDepartmentIfExists(String department) {
        String SQL_SELECT = "SELECT id from department where name=?";
        try (Connection conn = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
            preparedStatement.setString(1, department);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;


    }

    private void truncateTable(String tableName) {
        String SQL_INSERT = "DELETE  from " + tableName + " where `id`>0; \n";
        System.out.println(SQL_INSERT);
        try (
                Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                Statement statement = connection.createStatement();
        ) {
            statement.executeUpdate(SQL_INSERT);
        } catch (SQLException e) {
            System.out.println("Could not truncate" + e);
        }
    }

    private void insertRoleUser(Long userId, Long roleId) {
        String SQL_INSERT = "INSERT INTO `role_user`(`user_id`,`role_id`) VALUES (?,?)";
        try (
                Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                PreparedStatement statement = connection.prepareStatement(SQL_INSERT);
        ) {
            statement.setLong(1, userId);
            statement.setLong(2, roleId);
            statement.executeUpdate();
            // ...
        } catch (SQLException e) {
            System.out.println("CONTROL: TASK AND USER ALREADY EXISTS");
        }

    }

    private Long findUserIdByLogin(String username) {
        String SQL_SELECT = "SELECT id from user where username=?";
        try (Connection conn = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
            preparedStatement.setString(1, username);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }



    private Long selectRoleByName(String roleName) {
        String SQL_SELECT = "SELECT id from role where name=?";
        System.out.println(roleName);
        try (Connection conn = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
            preparedStatement.setString(1, roleName);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("ERROR OCCURED WHILE SELECTING ROLE");
    }

    private Long insertUser(Long id, String name, String username, String password, Long groupingId, Long positionId, boolean active) {
        String SQL_INSERT = "INSERT INTO `user` (`name`,`password`, `username`,`position_id`,`active`,`created_at`,updated_at, grouping_id) VALUES (?,?, ?,?,?,?,?,?);";
        if (name != null || !name.isEmpty())
            try (
                    Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                    PreparedStatement statement = connection.prepareStatement(SQL_INSERT,
                            Statement.RETURN_GENERATED_KEYS);
            ) {
                statement.setString(1, name);
                statement.setString(2, password);
                statement.setString(3, username);
                statement.setLong(4, positionId);
                statement.setBoolean(5, active);
                statement.setDate(6, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                statement.setDate(7, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                if (groupingId == null) {
                    statement.setNull(8, Types.NULL);
                } else
                    statement.setLong(8, groupingId);

                int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Creating user failed, no rows affected.");
                }

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();

            }
        throw new RuntimeException("user is null or empty");
    }

    private Long insertPosition(Long id, String name, Long departmentId, Long parentId) {
        String SQL_INSERT = "INSERT INTO `position` (`name`, `department_id`,`parent_id`,`created_at`,`updated_at`) VALUES (?, ?,?,?,?);";
        if (name != null || !name.isEmpty())
            try (
                    Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                    PreparedStatement statement = connection.prepareStatement(SQL_INSERT,
                            Statement.RETURN_GENERATED_KEYS);
            ) {
                statement.setString(1, name);
                if (departmentId != null)
                    statement.setLong(2, departmentId);
                else statement.setNull(2, Types.BIGINT);
                if (parentId != null)
                    statement.setLong(3, parentId);
                else statement.setNull(3, Types.BIGINT);
                statement.setDate(4, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                statement.setDate(5, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));

                int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Creating position failed, no rows affected.");
                }

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating position failed, no ID obtained.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();

            }
        throw new RuntimeException("POSITION is null or empty");
    }

    private Long insertDepartment(String name, String abbreviation) {
        String SQL_INSERT = "INSERT INTO `department` (`name`, `abbreviation`,`created_at`,`updated_at`) VALUES (?, ?,?,?);";
        if (name != null && !name.isEmpty()) {
            try (
                    Connection connection = DriverManager.getConnection(DbConstants.jdbcURL, DbConstants.username, DbConstants.password);
                    PreparedStatement statement = connection.prepareStatement(SQL_INSERT,
                            Statement.RETURN_GENERATED_KEYS);
            ) {
                statement.setString(1, name);
                statement.setString(2, abbreviation);
                statement.setDate(3, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                statement.setDate(4, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));

                int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Creating Department failed, no rows affected.");
                }

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating Department failed, no ID obtained.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    class UserAndDuplicate {
        private Long userIdFromExcell;
        private Long userIdFromDatabase;

        public UserAndDuplicate(Long userIdFromExcell, Long userIdFromDatabase) {
            this.userIdFromExcell = userIdFromExcell;
            this.userIdFromDatabase = userIdFromDatabase;
        }

        @Override
        public String toString() {
            return "UserAndDuplicate{" +
                    "Id юзера в экселе=" + userIdFromExcell +
                    ", Id уже существующего юзера в системе=" + userIdFromDatabase +
                    '}';
        }
    }


}
