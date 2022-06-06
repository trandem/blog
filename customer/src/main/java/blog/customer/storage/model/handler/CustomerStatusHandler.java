package blog.customer.storage.model.handler;

import blog.customer.storage.model.glosory.CustomerStatus;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(CustomerStatus.class)
public class CustomerStatusHandler implements TypeHandler<CustomerStatus> {
    @Override
    public void setParameter(PreparedStatement ps, int i, CustomerStatus parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null){
            ps.setNull(i, jdbcType.TYPE_CODE);
        }else {
            ps.setByte(i,parameter.getValue());
        }
    }

    @Override
    public CustomerStatus getResult(ResultSet rs, String columnName) throws SQLException {
        byte side = rs.getByte(columnName);
        if (rs.wasNull()){
            return null;
        }else{
            return CustomerStatus.valueOf(side);
        }
    }

    @Override
    public CustomerStatus getResult(ResultSet rs, int columnIndex) throws SQLException {
        byte side = rs.getByte(columnIndex);
        if (rs.wasNull()){
            return null;
        }else{
            return CustomerStatus.valueOf(side);
        }
    }

    @Override
    public CustomerStatus getResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte side = cs.getByte(columnIndex);
        if (cs.wasNull()){
            return null;
        }else{
            return CustomerStatus.valueOf(side);
        }
    }
}
