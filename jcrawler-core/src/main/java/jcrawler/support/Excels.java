package jcrawler.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

public class Excels {
	
	public static <T> Multimap<String, T> readMultimap(File file, RowGetter<T> rowGetter) {
	    Workbook wb = null;
	    Sheet sheet = null;
	    Row row = null;
	    
	    try {
	    	POIFSFileSystem fs = new POIFSFileSystem(file);
            wb = new HSSFWorkbook(fs);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
	    	try {
				wb = new XSSFWorkbook(file);
			} catch (IOException e1) {
				e1.printStackTrace();
				return null;
			} catch (Exception e1) {
				e1.printStackTrace();
				return null;
			}
	    }
	    
		Multimap<String, T> dataMap = HashMultimap.create();
		try {
			int numSheet = wb.getNumberOfSheets();
			for (int i = 0; i < numSheet; i++) {
				sheet = wb.getSheetAt(i);
				String sheetName = sheet.getSheetName();
				int numRow = sheet.getLastRowNum();
				int end = rowGetter.end();
				for (int j = rowGetter.start(), size = Math.min(numRow, end); j <= size; j++) {
					try {
						row = sheet.getRow(j);
						dataMap.put(sheetName, rowGetter.getRow(row));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} finally {
			IOUtils.closeQuietly(wb);
		}
		return dataMap;
	}
	
	public static <T> Table<String, String, T> readTable(File file,
			int keyIndex, RowGetter<T> rowGetter) {
	    Workbook wb = null;
	    Sheet sheet = null;
	    Row row = null;
	    
	    try {
	    	POIFSFileSystem fs = new POIFSFileSystem(file);
            wb = new HSSFWorkbook(fs);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
	    	try {
				wb = new XSSFWorkbook(file);
			} catch (IOException e1) {
				e1.printStackTrace();
				return null;
			} catch (Exception e1) {
				e1.printStackTrace();
				return null;
			}
	    }
	    
	    Table<String, String, T> dataTable = HashBasedTable.create();
	    try {
	    	int numSheet = wb.getNumberOfSheets();
	    	for (int i = 0; i < numSheet; i++) {
	    		sheet = wb.getSheetAt(i);
	    		String sheetName = sheet.getSheetName();
	    		int numRow = sheet.getLastRowNum();
	    		int end = rowGetter.end();
	    		for (int j = rowGetter.start(), size = Math.min(numRow, end); j <= size; j++) {
	    			try {
	    				row = sheet.getRow(j);
	    				dataTable.put(sheetName, cellString(row.getCell(keyIndex)), rowGetter.getRow(row));
	    			} catch (Exception e) {
	    				e.printStackTrace();
	    			}
	    		}
	    	}
	    } finally {
	    	IOUtils.closeQuietly(wb);
	    }
		return dataTable;
	}
	
	public static <T> void writeMultimap(Multimap<String, T> dataMap, File target, RowSetter<T> rowSetter) {
//		HSSFWorkbook wb = new HSSFWorkbook();
		XSSFWorkbook wb = new XSSFWorkbook();
		Sheet sheet = null;
		Row row = null;

		Set<String> sheetNames = dataMap.keySet();
		for (String sheetName : sheetNames) {
			sheet = wb.createSheet(sheetName);
			int numRow = 0;
			row = sheet.createRow(numRow++);
			rowSetter.setHead(sheetName, row);
			for (T object : dataMap.get(sheetName)) {
				try {
					row = sheet.createRow(numRow++);
					rowSetter.setRow(sheetName, row, object);
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
			IOUtils.closeQuietly(wb);
			IOUtils.closeQuietly(out);
		}
	}
	
	public static <T> void writeTable(Table<String, String, T> dataTable, File target, RowSetter<T> rowSetter) {
//		HSSFWorkbook wb = new HSSFWorkbook();
		XSSFWorkbook wb = new XSSFWorkbook();
		Sheet sheet = null;
		Row row = null;

		Set<String> sheetNames = dataTable.rowKeySet();
		for (String sheetName : sheetNames) {
			sheet = wb.createSheet(sheetName);
			int numRow = 0;
			Map<String, T> records = dataTable.row(sheetName);
			for (T object : records.values()) {
				try {
					row = sheet.createRow(numRow++);
					rowSetter.setRow(sheetName, row, object);
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
			IOUtils.closeQuietly(wb);
			IOUtils.closeQuietly(out);
		}
	}
	
	public static String cellString(Cell cell) {
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
	
  public static String textValue(Object input, String dateFormat) {
    if (input == null) {
      return StringUtils.EMPTY;
    }
    if (input instanceof String) {
      return StringUtils.trim((String) input);
    }
    if (input instanceof Date) {
      if (StringUtils.isBlank(dateFormat)) {
        dateFormat = "yyyy-MM-dd";
      }
      SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
      return sdf.format((Date) input);
    }
    return StringUtils.trim(input.toString());
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
		int end();
		T getRow(Row row);
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
		void setHead(String sheetName, Row row);
		void setRow(String sheetName, Row row, T object);
	}

}
