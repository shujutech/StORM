package biz.shujutech.example;

import biz.shujutech.base.App;
import biz.shujutech.base.Connection;
import biz.shujutech.base.Hinderance;
import biz.shujutech.db.object.ObjectBase;
import org.joda.time.DateTime;

/**
 *
 */
public class Simple {

	public static void main(String[] args) throws Exception {
		ObjectBase objectDb = new ObjectBase();
		Connection conn = null;
		try {
			String[] args1 = { "stormi.properties" };
			objectDb.setupApp(args1);
			objectDb.setupDb();
			conn = objectDb.getConnPool().getConnection();

			LeaveForm leaveForm = (LeaveForm) ObjectBase.CreateObject(conn, LeaveForm.class);
			leaveForm.setLeaveType("Annual");
			leaveForm.setDateApplied(new DateTime());
			leaveForm.setDateApplied(new DateTime().plusDays(1));
			leaveForm.persistCommit(conn);
			App.logInfo(Simple.class, "Successfully save leave form into the database!");
		} catch (Exception ex) {
			App.logEror(new Hinderance(ex, "Application encounter fatal error, application is aborting...."));
		} finally {
			if (conn != null) {
				objectDb.getConnPool().freeConnection(conn);
			}
		}		
	}

}
