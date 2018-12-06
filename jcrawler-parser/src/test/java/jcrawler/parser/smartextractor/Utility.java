package jcrawler.parser.smartextractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 分析中需要调用的一些静态方法，工具集
 * @author Lamfeeling
 */
public class Utility {

    private static final String[] LARGE_NODES = {"DIV", "TABLE"};
    private static final String[] TABLE_NODES = {"TR", "TD"};
    private static final String[] INFO_NODE = {"P", "SPAN", "H1", "H2", "B", "I"};
    public static final String[] HEADING_TAGS = {"TITLE", "H1", "H2", "H3", "H4", "H5", "H6", "H7"};
    private static final String[] INVALID_TAGS = {"STYLE", "COMMENT", "SCRIPT", "OPTION","IFRAME"};
    private static final String[] SPACING_TAGS = {"BR", "SPAN"};
    private static final String LINK_NODE = "A";

    /**
     * 万能的Web下载方法，可以自动分析编码
     * @param URL
     * @return
     */
    public static String getWebContent(String URL) throws IOException {
        String s = "";
        try {
            //从url打开stream
            InputStream in = null;
            HttpURLConnection conn = (HttpURLConnection) new URL(URL).openConnection();
            in = conn.getInputStream();
            //尝试从http头中获取字符集
            String contentType = conn.getHeaderField("Content-Type").toLowerCase();
            String encoding = "utf-8";
            boolean charsetFound = false;
            if (contentType.contains("charset")) {
                encoding = contentType.split("charset=")[1];
                charsetFound = true;
            }
            //如果没有的话,读取头1024个字符，检查html的header
            byte[] buf = new byte[1024];
            if (!charsetFound) {
                int len = in.read(buf);
                while (len <= 32) {
                    len = in.read(buf);
                }
                String header = new String(buf, 0, len);
                Pattern p = Pattern.compile(".*<meta.*content=.*charset=.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(header);
                if (m.matches()) {
                    encoding = header.toLowerCase().split("charset=")[1].replaceAll("[^a-z|1-9|\\-]", " ").split("\\s+")[0];
                } else {
                    //如果没有的话，直接用gb2312解码
                    encoding = "gb2312";
                }
            }
            if(encoding.equals("none")){
            	encoding = "gb2312";
            }
            //开始读取内容正文。
            BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding));
            String header = new String(buf, encoding);
            //add the header to our content
            StringBuffer sb = new StringBuffer(header);
            char[] charBuf = new char[2048];
            int len = br.read(charBuf);
            while (len != -1) {
                sb.append(charBuf, 0, len);
                len = br.read(charBuf);
            }
            br.close();
            s = sb.toString();
            if(!s.trim().startsWith("<")){
                s = "<"+s.trim();
            }
        } catch (IOException ex) {
            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex);
        }
        return s;
    }

    /**
     * 过滤掉HTML文本中的一些非常规符号。<b>任何获取HTML文本的调用都必须事先用这个方法过滤</b>
     * @param text 需要过滤的文本
     * @return 需要过滤的文本。
     */
    public static String filter(String text) {
        text = text.replaceAll("[^\u4e00-\u9fa5|a-z|A-Z|0-9|０-９,.，。:；：><?…》《\"”“!\\-©|\\s|\\@]", " ");
        text = text.replaceAll("[【】]", " ");
        text = text.replaceAll("[\r\n]+", "\r\n");
        text = text.replaceAll("\n+", "\n");
        text = text.replaceAll("\\|", "");
        text = text.replaceAll("\\s+", " ");
        text = text.trim();
        return text;
    }

    /**
     * 是否是Table所包含的的元素？
     * @param e
     * @return 是否是Table所包含的的元素？
     */
    public static boolean isTableNodes(Element e) {
        for (String s : TABLE_NODES) {
            if (e.getTagName().equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是大的节点？ 例如div, table?
     * @param e
     * @return 是否是大的节点？
     */
    public static boolean isLargeNode(Element e) {
        for (String s : LARGE_NODES) {
            if (e.getTagName().equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是包含信息的节点？
     * @param e
     * @return 是否是包含信息的节点？
     */
    public static boolean isInfoNode(Element e) {
        for (String s : INFO_NODE) {
            if (e.getTagName().equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是标题节点？H1-H6
     * @param e
     * @return 是否是标题节点？
     */
    public static boolean isHeading(Element e) {
        for (String s : HEADING_TAGS) {
            if (e.getTagName().equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是对文本分析无用的节点？
     * @param e
     * @return 是否是对文本分析无用的节点？
     */
    public static boolean isInvalidElement(Element e) {
        for (String s : INVALID_TAGS) {
            if (e.getTagName().equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是超链接节点？
     * @param e
     * @return 是否是超链接节点？
     */
    public static boolean isLinkNode(Element e) {
        if (e.getTagName().equals(LINK_NODE)) {
            return true;
        }
        return false;
    }

    /**
     * 当前节点下有多少超链接节点？
     * @param e
     * @return
     */
    public static int numLinkNode(Element e) {
        int num = isLinkNode(e) ? 1 : 0;
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                num += numLinkNode((Element) children.item(i));
            }
        }
        return num;
    }

    /**
     * Judge whether we need to warp the current Element after appended it to String Buffer.
     * @param e
     * @return
     */
    public static boolean needWarp(Element e) {
        if (isHeading(e) || e.getTagName().equals("P") || isTableNodes(e) || isLargeNode(e)) {
            return true;
        }
        return false;
    }

    /**
     * Judge whehter we should add one space when facing the specific element
     * @param e
     * @return
     */
    public static boolean needSpace(Element e) {
        for (String s : SPACING_TAGS) {
            if (e.getTagName().equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * wether this tag will effect the html appearance on browser？
     * @param e
     * @return wether this tag will effect the html appearance on browser？
     */
    public static boolean isAppearanceTag(Element e) {
        //headings
        if (e.getTagName().matches("H[1-9]")) {
            return true;
        }
        //colored fonts
        if (e.getTagName().equals("FONT") &&
                !e.getAttribute("COLOR").equals("")) {
            return true;
        }
        //stronged texts
        if (e.getTagName().matches("[B|I|STRONG]")) {
            return true;
        }
        return false;
    }

    public static boolean containsInput(Element e) {
        NodeList inputs = e.getElementsByTagName("INPUT");
        if (inputs.getLength() != 0) {
            boolean allhidden = true;
            for (int i = 0; i < inputs.getLength(); i++) {
                Element ei = (Element) inputs.item(i);
                if (!ei.getAttribute("TYPE").toLowerCase().equals("hidden")) {
                    allhidden = false;
                    break;
                }
            }
            return !allhidden;
        }
        return false;
    }

    /**
     * Whether this text contains some regular noise on Internet?
     * @param text
     * @return Whether this text contains some regular noise on Internet? 
     */
    public static boolean containsNoise(String text) {
        if (text.toLowerCase().contains("copyright") ||
                text.toLowerCase().contains("all rights reserved") ||
                text.toLowerCase().contains("版权所有") ||
                text.toLowerCase().contains("©") ||
                text.toLowerCase().contains("上一页") ||
                text.toLowerCase().contains("下一页") ||
                text.toLowerCase().contains("ICP备")) {
            return true;
        }
        return false;
    }
    
    /**
     * 删除文档中的一些显然不会包含主题信息的节点，例如script,style,等等，它们将影响我们的文本抽取器的分析。
     * @param e 所需要清楚地w3c节点
     */
    public static Node cleanup(Element e) {
		NodeList c = e.getChildNodes();
		for (int i = 0; i < c.getLength(); i++) {
			if (c.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element t = (Element) c.item(i);
				if (Utility.isInvalidElement(t)) {
					e.removeChild(c.item(i));
				} else {
					cleanup(t);
				}
			}
		}
		return e;
	}
}
