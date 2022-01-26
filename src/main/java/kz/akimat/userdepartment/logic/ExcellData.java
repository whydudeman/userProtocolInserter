package kz.akimat.userdepartment.logic;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExcellData {
    public Long id;
    public String name;
    public String username;
    public String password;
    public String position;
    public String department;
    public String abbreviation;
    public String grouping;
    public Long parentId;
    public List<String> roles;
    public boolean firstGroup = false;
    public boolean secondGroup = false;
    public boolean thirdGroup = false;
    public boolean forthGroup = false;

    public ExcellData(Row row) {
        this.name = getStringFromRowByIndex(row.getCell(1));
        this.username = getStringFromRowByIndex(row.getCell(2));
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String notEncodedPassword = getStringFromRowByIndex(row.getCell(3));
        this.password = passwordEncoder.encode(notEncodedPassword);
        this.position = getStringFromRowByIndex(row.getCell(4));
        this.department = getStringFromRowByIndex(row.getCell(5));
        this.abbreviation = getStringFromRowByIndex(row.getCell(6));
        this.grouping = getStringFromRowByIndex(row.getCell(7));
        this.parentId = getLongValueFromCell(row.getCell(8));

        String roles = getStringFromRowByIndex(row.getCell(9));

        if (roles != null && !roles.isEmpty()) {
            this.roles = new ArrayList<>(Arrays.asList(roles.trim().split(",")));
        }

//        firstGroup = getLongValueFromCell(row.getCell(10)) != null;
//        secondGroup = getLongValueFromCell(row.getCell(11)) != null;
//        thirdGroup = getLongValueFromCell(row.getCell(12)) != null;
//        forthGroup = getLongValueFromCell(row.getCell(13)) != null;
    }

    private String getStringFromRowByIndex(Cell cell) {
        if (cell != null)
            if (cell.getCellType().equals(CellType.STRING)) {
                if (cell.getStringCellValue().trim().toLowerCase().startsWith("null") || cell.getStringCellValue().trim().isEmpty())
                    return null;
                return cell.getStringCellValue().trim();
            }
        return null;
    }

    private Long getLongValueFromCell(Cell cell) {
        if (cell != null) {
            if (cell.getCellType().equals(CellType.STRING)) {
                if (cell.getStringCellValue().toLowerCase().trim().startsWith("null") || cell.getStringCellValue().isEmpty())
                    return null;
                return Long.valueOf(cell.getStringCellValue());
            }
            if (cell.getCellType().equals(CellType.NUMERIC))
                return (long) cell.getNumericCellValue();
        }
        return null;
    }

    @Override
    public String toString() {
        return "ExcellData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", position='" + position + '\'' +
                ", department='" + department + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                ", grouping='" + grouping + '\'' +
                ", parentId=" + parentId +
                ", roles=" + roles +
                ", firstGroup=" + firstGroup +
                ", secondGroup=" + secondGroup +
                ", thirdGroup=" + thirdGroup +
                ", forthGroup=" + forthGroup +
                '}';
    }
}
