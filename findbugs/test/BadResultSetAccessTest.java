
import java.sql.ResultSet;
import java.sql.SQLException;

public class BadResultSetAccessTest
{
	public void test0( ResultSet rs ) throws SQLException {
		for (int i = 0; i < 5; i++) {
			System.out.println(rs.getString(i));
		}	
	}

	public void test1( ResultSet rs ) throws SQLException {
		int i = rs.getInt(0);
		i++;
		rs.updateInt(0, i );
	}
	
	public void test2( ResultSet rs ) throws SQLException {
		String s = rs.getString(0);
		s = s.substring(1);
		rs.updateString(0, s );
	}
	
	public void test3( ResultSet rs ) throws SQLException {
		String s = rs.getString("foo");
		s = s.substring(1);
		rs.updateString("foo", s );
	}
	
	public void test4( ResultSet rs ) throws SQLException {
		rs.updateBinaryStream( 1, null, 0 );
	}
}