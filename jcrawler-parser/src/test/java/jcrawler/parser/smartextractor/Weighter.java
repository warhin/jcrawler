package jcrawler.parser.smartextractor;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * 一个HTML标记窗在此文本抽取系统中的抽象。
 * 可以用它来获取此标记本身的一些特征参数，例如超文本密度等等。
 */
public class Weighter {

    private Node node = null;
    private String text = "";
    private String anchorText = "";
    private int numInfoNodes = 0;

    /**
     * 用一个标准的w3cNode对象来生成一个Tag对象。
     * @param node w3cNode对象。
     */
    public Weighter(Node node) {
        this.node = node;
        text = getInnerText(node, false);
        anchorText = getAnchorText();

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            numInfoNodes = getNumInfoNode((Element) node);
        }
    }

    /**
     * 平滑函数，用来平滑我们的算法中的一些值。目前为了效率，我们使用非常粗糙的解题函数
     * @param x 参数
     * @return 平滑后的参数
     */
    private static double fn(double x) {
        if (x > 0.8f) {
            return 0.8f;
        }
        return x;
    }

    /**
     * 通过所给的HTML全文参数，计算该链接在该HTML文档中的权重。
     * @param totalT HTML文档的总字数。
     * @param totalA HTML文档的总超链接字数。
     * @param totalNumInfoNodes HTML文档的<a href="#">infoNode</a>个数。
     * @return 该标签在所给参数的HTML文档当中的权重
     */
    public double weight(int totalT, int totalA, int totalNumInfoNodes) {
        double weight = 0;
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element e = (Element) node;
            weight += Utility.isTableNodes(e) ? .1 : 0;
            weight += Utility.isLargeNode(e) ? .1 : 0;
            weight += 0.2 * fn(numInfoNodes / (double) (totalNumInfoNodes));
            weight -= Utility.containsInput(e) ? .5 : 0;
        }

        if (Utility.containsNoise(text)) {
            weight -= 0.5;
        }

        weight += 1.0 - anchorDensity();
        weight += share(totalA, totalT);
        return weight;
    }

    /**
     * 定义：锚文本和整体文本的比值
     * @note 特别的：如果text或者anchorText为空，那么这个anchorDensity为0
     * @return
     */
    private double anchorDensity() {
        int anchorLen = anchorText.length();
        int textLen = text.length();
        if (anchorLen == 0 || textLen == 0) {
            return 0;
        }
        return anchorLen / (double) textLen;
    }

    /**
     * 通过所给的HTML参数计算该标记在该HTML文档中所站的份额比例。
     * 计算方法是，alpha*文字所占的比例-beta超链接所占比例，公式参数详见函数内
     * @param totalA 该HTML文档包含的超链接文本的字数。
     * @param totalT 该HTML文档包含的文本字数。
     * @return 这个标记所占的比例。
     */
    private double share(int totalA, int totalT) {
        if (totalA == 0) {
            return 1.6 * fn((double) text.length() / totalT);
        }
        return 1.6 * fn((double) text.length() / totalT) - .8 * anchorText.length() / totalA;
    }

    /**
     *  获取这个标签包含的文本
     * @param viewMode 是否依照浏览器显示的方式进行优化（将加入额外的空格和换行）？
     * @return 这个标签所包含的实际文本。
     */
    public String getInnerText(boolean viewMode) {
        if (viewMode) {
            return getInnerText(node, viewMode);
        } else {
            return text;
        }
    }
    
    public String getInnerText2(boolean viewMode) {
        if (viewMode) {
            return getInnerText2(node, viewMode);
        } else {
            return text;
        }
    }

    /**
     * 这个标签包含的infoNode的个数。
     * @return 这个标签包含的infoNode的个数。
     */
    public int getNumInfoNodes() {
        return numInfoNodes;
    }

    /**
     * 这个标签所包含的锚文本字符
     * @return 这个标签所包含的锚文本字符
     */
    private String getAnchorText() {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            return getAnchorText((Element) node);
        }
        return "";
    }

    /**
     * 获取这个类里的w3cNode对象
     * @return
     */
    public Node getNode() {
        return node;
    }

    /**
     * 获取指定标签内所包含的有效文字
     * @param node 所指定的标签
     * @param viewMode viewMode 是否依照浏览器显示的方式进行优化（将加入额外的空格和换行）？
     * @return 获取指定标签内所包含的有效文字
     */
    public static String getInnerText(Node node, boolean viewMode) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            return Utility.filter(((Text) node).getData());
        }

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            if (Utility.isInvalidElement(element)) {
                return "";
            }
            StringBuilder nodeText = new StringBuilder();
            //replace the line break with space,
            //beacause inappropriate line break may cause the paragraph corrupt.
            if (viewMode && element.getTagName().equals("BR")) {
                nodeText.append(" ");
            }
            //let the appearance tags stay
            if (viewMode && Utility.isHeading(element)) {
                nodeText.append("<" + element.getTagName() + ">");
            }
            NodeList list = element.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                String t = getInnerText(list.item(i), viewMode);
                //whether we need to add extra space?
                if (viewMode && Utility.needSpace(element)) {
                    t += " ";
                }
                nodeText.append(t);
            }
            if (viewMode && Utility.isHeading(element)) {
                nodeText.append("</" + element.getTagName() + ">");
            }
            //break the line, if the element is a REAL BIG tag, such as DIV,TABLE
            if (viewMode &&
                    Utility.needWarp(element) &&
                    nodeText.toString().trim().length() != 0) {
                nodeText.append("\r\n");
            }
            return nodeText.toString().replaceAll("[\r\n]+", "\r\n");
        }

        return "";
    }
    public static String getInnerText2(Node node, boolean viewMode) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            return Utility.filter(((Text) node).getData());
        }

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            if (Utility.isInvalidElement(element)) {
                return "";
            }
          //  System.out.println("Elements: "+element.getTagName());
            if (element.getTagName().equals("A")){
            	return "";
            }
            StringBuilder nodeText = new StringBuilder();
            //replace the line break with space,
            //beacause inappropriate line break may cause the paragraph corrupt.
            if (viewMode && element.getTagName().equals("BR")) {
                nodeText.append(" ");
            }
            //let the appearance tags stay
            if (viewMode && Utility.isHeading(element)) {
                nodeText.append("<" + element.getTagName() + ">");
            }
            NodeList list = element.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                String t = getInnerText2(list.item(i), viewMode);
                //whether we need to add extra space?
                if (viewMode && Utility.needSpace(element)) {
                    t += " ";
                }
                nodeText.append(t);
            }
            if (viewMode && Utility.isHeading(element)) {
                nodeText.append("</" + element.getTagName() + ">");
            }
            //break the line, if the element is a REAL BIG tag, such as DIV,TABLE
            if (viewMode &&
                    Utility.needWarp(element) &&
                    nodeText.toString().trim().length() != 0) {
                nodeText.append("\r\n");
            }
            return nodeText.toString().replaceAll("[\r\n]+", "\r\n");
        }

        return "";
    }

    /**
     * 这个标签所包含的锚文本字符
     * @param e 所指定的w3cHTML元素
     * @return 这个标签所包含的锚文本字符
     */
    public static String getAnchorText(Element e) {
        StringBuilder anchorLen = new StringBuilder();
        // get anchor text length
        NodeList anchors = e.getElementsByTagName("A");
        for (int i = 0; i < anchors.getLength(); i++) {
            anchorLen.append(getInnerText(anchors.item(i), false));
        }
        return anchorLen.toString();
    }

    /**
     * 当前这个节点下包含多少个InfoNode?
     * @param e 所给定的w3c元素
     * @return 当前这个节点下包含多少个InfoNode?
     */
    public static int getNumInfoNode(Element e) {
        int num = Utility.isInfoNode(e) ? 1 : 0;
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                num += getNumInfoNode((Element) children.item(i));
            }
        }
        return num;
    }
}
