package jcrawler.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

public class Excels {
	
	public static <T> Multimap<String, T> readMultimap(InputStream is, RowGetter<T> rowGetter) {
		POIFSFileSystem fs = null;
	    HSSFWorkbook wb = null;
	    HSSFSheet sheet = null;
	    HSSFRow row = null;
	    
	    try {
            fs = new POIFSFileSystem(is);
            wb = new HSSFWorkbook(fs);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
	    
	    Multimap<String, T> dataMap = HashMultimap.create();
	    int numSheet = wb.getNumberOfSheets();
	    for (int i = 0; i < numSheet; i++) {
	    	sheet = wb.getSheetAt(i);
	    	String sheetName = sheet.getSheetName();
	    	int numRow = sheet.getLastRowNum();
	    	for (int j = rowGetter.start(); j <= numRow; j++) {
	    		try {
	    			row = sheet.getRow(j);
	    			dataMap.put(sheetName, rowGetter.getRow(row));
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
	    	}
	    }
	    return dataMap;
	}
	
	public static <T> Table<String, String, T> readTable(InputStream is,
			int keyIndex, RowGetter<T> rowGetter) {
		POIFSFileSystem fs = null;
	    HSSFWorkbook wb = null;
	    HSSFSheet sheet = null;
	    HSSFRow row = null;
	    
	    try {
            fs = new POIFSFileSystem(is);
            wb = new HSSFWorkbook(fs);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
	    
	    Table<String, String, T> dataTable = HashBasedTable.create();
	    int numSheet = wb.getNumberOfSheets();
	    for (int i = 0; i < numSheet; i++) {
	    	sheet = wb.getSheetAt(i);
	    	String sheetName = sheet.getSheetName();
	    	int numRow = sheet.getLastRowNum();
	    	for (int j = rowGetter.start(); j <= numRow; j++) {
	    		try {
	    			row = sheet.getRow(j);
	    			dataTable.put(sheetName, cellString(row.getCell(keyIndex)), rowGetter.getRow(row));
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
	    	}
	    }
		return dataTable;
	}
	
	public static <T> void writeMultimap(Multimap<String, T> dataMap, File target, RowSetter<T> rowSetter) {
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = null;
		HSSFRow row = null;

		Set<String> sheetNames = dataMap.keySet();
		for (String sheetName : sheetNames) {
			sheet = wb.createSheet(sheetName);
			int numRow = 0;
			row = sheet.createRow(numRow++);
			rowSetter.setHead(row);
			for (T object : dataMap.get(sheetName)) {
				try {
					row = sheet.createRow(numRow++);
					rowSetter.setRow(row, object);
				} catch (Exception e) {
	    			e.printStackTrace();
	    		}
			}
		}

		// 创建文件输出流，准备输出电子表格
		OutputStream out = null;
		try {
			out = new FileOutputStream(target);
			wb.write(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(out);
		}
	}
	
	public static <T> void writeTable(Table<String, String, T> dataTable, File target, RowSetter<T> rowSetter) {
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = null;
		HSSFRow row = null;

		Set<String> sheetNames = dataTable.rowKeySet();
		for (String sheetName : sheetNames) {
			sheet = wb.createSheet(sheetName);
			int numRow = 0;
			Map<String, T> records = dataTable.row(sheetName);
			for (T object : records.values()) {
				try {
					row = sheet.createRow(numRow++);
					rowSetter.setRow(row, object);
				} catch (Exception e) {
	    			e.printStackTrace();
	    		}
			}
		}

		// 创建文件输出流，准备输出电子表格
		OutputStream out = null;
		try {
			out = new FileOutputStream(target);
			wb.write(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(out);
		}
	}
	
	public static String cellString(HSSFCell cell) {
		if (cell == null) return null;
		String value = null;
		switch (cell.getCellType()) {
		case HSSFCell.CELL_TYPE_STRING:
			value = cell.getStringCellValue();
			break;
		case HSSFCell.CELL_TYPE_NUMERIC:
			value = String.valueOf(cell.getNumericCellValue());
			break;
		case HSSFCell.CELL_TYPE_BOOLEAN:
			value = String.valueOf(cell.getBooleanCellValue());
			break;
		case HSSFCell.CELL_TYPE_BLANK:
		default:
			value = null;
			break;
		}
		return value;
	}
	
	/**
	 * 读excel时的接口，将每行excel转换为数据对象T，指定起始行号和所有数据行row。
	 * 
	 * 一般从start位置开始转换，每行excel对应数据对象T的某个字段。
	 * 
	 * @author warhin.wang
	 *
	 * @param <T>
	 */
	public static interface RowGetter<T> {
		int start();
		T getRow(HSSFRow row);
	}

	/**
	 * 写excel时的接口，将数据对象T转换为excel行，设置excel的第一行head和所有数据行row。
	 * 
	 * 一般第一行head设置每列的名称，每行数据行的每列对应数据对象T的某个字段。
	 * 
	 * @author warhin.wang
	 *
	 * @param <T>
	 */
	public static interface RowSetter<T> {
		void setHead(HSSFRow row);
		void setRow(HSSFRow row, T object);
	}

}
