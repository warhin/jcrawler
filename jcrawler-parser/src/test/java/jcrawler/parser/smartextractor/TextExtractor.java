package jcrawler.parser.smartextractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @version 1.0
 * @author JINJUN
 * @modified by Liu Song
 * <pre>
 *      这个类将所有的正文文本从一篇HTML文档中提取出来，并且分成若干段落。
 *      @note 段落 是指一篇HTML文档中，语义相对集中的一段文字。
 *      每一个提取出来的段落将会获得一个权重。这个权重是该段落对HTML文档主题的贡献程度
 *
 *      Usage:
 *      BufferedReader reader = new BufferedReader(new FileReader(...));
 *		DOMParser parser = new DOMParser();
 *		parser.parse(new InputSource(reader));
 *		Document doc = parser.getDocument();

 *		TextExtractor extractor = new TextExtractor(doc);
 *  	String text = extractor.extract();
 *  	System.out.println(text);
 *  </pre>
 */
public class TextExtractor {

    /**
     * 这个标记最大允许的锚文本密度，该值目前为.5
     */
    public static final double MAX_ANCHOR_DEN = 0.5;
    /**
     * 该HTML文档中有多少正文文字？
     */
    private int totalTextLen = 0;
    /**
     * 该HTML文档中有多少超链接文字？
     */
    private int totalAnchorTextLen = 0;
    /**
     * 该HTML文档中有多少infoNodes?
     */
    private int totalNumInfoNodes = 0;
    /**
     * 标记列表
     */
    private List<Weighter> windowsList = new ArrayList<Weighter>();
    /**
      * w3cHTML文档模型
      */
    private Document doc;

    /**
     * 利用給定的W3C文檔對象模型構造一個TextExtractor
     * @param doc 所给定的W3C文档对象模型
     */
    public TextExtractor(Document doc) {
        super();
        this.doc = doc;

    }

    /**
     * 抽取HTML文本信息，并且分段，为每一段文本的主题相关性打分。
     * @return 所抽取出的主题信息
     */
    public String extract() {
        Node body = doc.getElementsByTagName("BODY").item(0);
        //cleanup, remove the invalid tags,        

        if (body != null) {
            totalTextLen = Weighter.getInnerText(body, false).length();
            // get anchor text length
            totalAnchorTextLen = Weighter.getAnchorText((Element) body).length();

            totalNumInfoNodes = Weighter.getNumInfoNode((Element) body);
            Utility.cleanup((Element) body);
            extractWindows(body);
        } else {
       
            extractWindows(doc);
        }

        String bodyText = "";
        if (windowsList.size() == 0) {
            bodyText = "";
        } else {
            //get the max score
            Collections.sort(windowsList, new Comparator<Weighter>() {

                public int compare(Weighter t1, Weighter t2) {
                    if (t1.weight(totalTextLen, totalAnchorTextLen, totalNumInfoNodes) > t2.weight(totalTextLen,
                            totalAnchorTextLen, totalNumInfoNodes)) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });

            int afterNumber = windowsList.size();
            do {
                afterNumber--;
            } while (afterNumber > 0 && windowsList.get(afterNumber).getInnerText(false).trim().length() < 60);
            Weighter max = windowsList.get(afterNumber);
            bodyText = max.getInnerText2(true);
        }
        return bodyText;
    }

    public String getTitle() {
        return Utility.filter(doc.getElementsByTagName("TITLE").item(0).getTextContent());
    }

    /**
     * 遍历每个Node对象，把每个Node都存储到taglist当中。
     * @param node 所需要遍历的w3cNode对象
     */
    private void extractWindows(Node node) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            return;
        }

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            if (Utility.isInvalidElement(element)) {
                return;
            }
            if (!node.getTextContent().trim().equals("")) //add the tags
            {
                windowsList.add(new Weighter(node));
            }
            NodeList list = element.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                extractWindows(list.item(i));
            }
        }
    }

}
