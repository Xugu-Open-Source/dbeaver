package org.jkiss.dbeaver.ext.xugu.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import com.xugu.parser.Parsing;

public class XuguToolTableExport implements IUserInterfaceTool{

	@Override
	public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
			throws DBException {
		// TODO Auto-generated method stub
		Connection currentConnection = null ;
		DataSource dataSource;
		String schemaNameString = "";
		File outputFolder = null;
		if(objects.iterator().hasNext()) {
			Schema  object = (Schema)objects.iterator().next();
			dataSource =  object.getDataSource();	
			schemaNameString = object.getName();
			try {
				currentConnection = dataSource.getDefaultInstance()
						.getDefaultContext(true)
						.getConnection(new LoggingProgressMonitor());
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		SimpleDateFormat sFormat=new SimpleDateFormat("yyyyMMddHHmmss");
		Calendar calendar=Calendar.getInstance();
		//获取系统当前时间并将其转换为string类型
		String fileName=sFormat.format(calendar.getTime());
		if(schemaNameString!=""&&schemaNameString!=null) {
			    outputFolder = new File(RuntimeUtils.getUserHomeDir().getAbsolutePath()+"\\"+schemaNameString+"_tables_"+fileName+".sql");
		}
		 byte[] b = new Parsing().getTableDDL(currentConnection,schemaNameString, Parsing.TableType.ALL).getBytes();
		 FileOutputStream fos = null;
		try {
			 fos = new FileOutputStream(outputFolder);	
			 fos.write(b);
			    DBeaverNotifications.showNotification(
	                    DBeaverNotifications.NT_RECONNECT,
	                    schemaNameString,
	                     "export  tables  success"+"\r\n"+outputFolder.getAbsolutePath(),
	                    DBPMessageType.INFORMATION,new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								Thread thread = Thread.currentThread();
								try {
									thread.sleep(3000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}				
							}
						});
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if(fos!=null) {
				 try {
					fos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		
		
	}
}
