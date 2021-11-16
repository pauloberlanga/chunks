package br.usp.nlp_np;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Unit test for simple App.
 */
public class AppTestBackup {

	private static String xmlTrainPath = "C:\\Users\\Paulo.Berlanga\\git-nlp\\nlp-np\\src\\test\\xml\\train.xml";
	private static String outputTrainPath = "C:\\Users\\Paulo.Berlanga\\git-nlp\\nlp-np\\src\\test\\txt\\train_annotated.txt";

	private static String xmlDevPath = "C:\\Users\\Paulo.Berlanga\\git-nlp\\nlp-np\\src\\test\\xml\\dev.xml";
	private static String outputDevPath = "C:\\Users\\Paulo.Berlanga\\git-nlp\\nlp-np\\src\\test\\txt\\dev_annotated.txt";

	private static String xmlTestPath = "C:\\Users\\Paulo.Berlanga\\git-nlp\\nlp-np\\src\\test\\xml\\test.xml";
	private static String outputTestPath = "C:\\Users\\Paulo.Berlanga\\git-nlp\\nlp-np\\src\\test\\txt\\test_annotated.txt";

	@Before
	public void clearFiles() throws IOException {
		File outputTrainFile = new File(outputTrainPath);
    	if (outputTrainFile.delete()) {
    		outputTrainFile.createNewFile();
    	}
		File outputDevFile = new File(outputDevPath);
    	if (outputDevFile.delete()) {
    		outputDevFile.createNewFile();
    	}
		File outputTestFile = new File(outputTestPath);
    	if (outputTestFile.delete()) {
    		outputTestFile.createNewFile();
    	}
	}

    @Test
    public void processFiles() {
    	AppTestBackup.execute(xmlTrainPath, outputTrainPath);
    	AppTestBackup.execute(xmlDevPath, outputDevPath);
    	AppTestBackup.execute(xmlTestPath, outputTestPath);
    }

    private static void execute(String xmlFilePath, String outputFilePath) {
    	try {
            InputStream inputStream = new FileInputStream(xmlFilePath);
            Reader reader = new InputStreamReader(inputStream, "ISO-8859-1");
            InputSource is = new InputSource(reader);
            is.setEncoding("ISO-8859-1");

        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder builder = factory.newDocumentBuilder();
        	Document document = builder.parse(is);
        	XPathFactory xPathFactory = XPathFactory.newInstance();
        	XPath xPath = xPathFactory.newXPath();
        	XPathExpression extract = xPath.compile("/fs/extract");
        	//XPathExpression extract = xPath.compile("/fs/extract[@sel='true']");

        	int x = 0, y = 0, z = 0;
        	NodeList extractList = (NodeList) extract.evaluate(document, XPathConstants.NODESET);
        	for (Node node : AppTestBackup.iterable(extractList)) {
        		StringWriter writer = new StringWriter();
        		Transformer transformer = TransformerFactory.newInstance().newTransformer();
        		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        		transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        		transformer.transform(new DOMSource(node), new StreamResult(writer));

        		try {
            		if (AppTestBackup.containsNPSubject(writer.toString(), outputFilePath)) {
                		x++;
            		} else {
                		y++;
            		}
        		} catch (Exception ex) {
        			z++;
        		}
        	}
			System.out.println("-----");
			System.out.println("X: " + x);
			System.out.println("Y: " + y);
			System.out.println("Z: " + z);
			System.out.println("-----");

    	} catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }

    private static boolean containsNPSubject(String xml, String outputFilePath) throws Exception {    	
    	String[] source = AppTestBackup.collectSource(xml);
    	String sourceId = source[0].trim() + source[1].trim();

    	try {
//        	List<String> sourceTokens = Arrays.asList(Arrays.copyOfRange(source, 2, source.length));
        	List<String> treeTokens = AppTestBackup.collectTree(xml, false);
        	List<String> npTokens = AppTestBackup.collectTree(xml, true);
    		if (npTokens.size() == 0) {
    	        return false;
    		}
        	List<String> annotations = AppTestBackup.annotate(treeTokens, npTokens);

    		if (npTokens.size() == 0) {
    			AppTestBackup.write(sourceId, "--------------------------------------------------------", outputFilePath);
    			AppTestBackup.write("A", annotations.toString(), outputFilePath);
    			AppTestBackup.write("S", treeTokens.toString(), outputFilePath);
    			AppTestBackup.write("T", npTokens.toString(), outputFilePath);
    			return true;
    		} else {
    	        System.out.println(sourceId);
    			throw new Exception();
    		}
    	} catch (IndexOutOfBoundsException ex) {
	        System.out.println(sourceId);
			throw new Exception();
    	}
    }

    private static void write(String info, String text, String outputFilePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true));
        writer.write(info + " : " + text.replace(", ", " ") + "\n");
        writer.close();

//        System.out.println(info + " : " + text);
    }

    private static List<String> annotate(List<String> treeTokens, List<String> npTokens) {
    	List<String> annotations = new ArrayList<>();
    	for (String treeToken : treeTokens) {
    		String annotation = "-";
    		if (npTokens.size() > 0 && npTokens.get(0).equals(treeToken)) {
        		annotation = "X";
        		npTokens.remove(0);
    		}
    		String format = String.join("", Collections.nCopies(treeToken.length() - 1, " "));
    		annotations.add(annotation + format);
    	}
    	return annotations;
    }

    private static List<String> treeReps(List<String> treeTokens) {
    	List<String[]> reps = AppTestBackup.replacements();

    	for (int i = 0; i < treeTokens.size(); i++) {
    		for (String[] rep : reps) {
        		if (treeTokens.get(i).equals(rep[0]) && treeTokens.get(i+1).equals(rep[1])) {
        			treeTokens.remove(i+1);
        			treeTokens.set(i, rep[2]);
        		}
    		}
    	}
    	return treeTokens;
    }

    private static String[] collectSource(String xml) throws XPathExpressionException {
    	InputSource extract = new InputSource(new StringReader(xml));
    	XPathFactory xPathFactory = XPathFactory.newInstance();
    	XPath xPath = xPathFactory.newXPath();

    	XPathExpression source = xPath.compile("//source/text()");
    	String sourceResult = (String) source.evaluate(extract, XPathConstants.STRING);

    	return AppTestBackup.sourceReps(sourceResult).split("\\s+");
    }

    private static String sourceReps(String input) {
    	return input
    			.replace("-", "- ")
    			.replace("'", "' ")
    			.replace(".\n", " .\n")
    			.replace("?", " ?")
    			.replace("!", " !")
    			.replace(", ", " , ")
    			.replace(";", " ;")
    			.replace(":", " :")
    			.replace("/", " ")
    			.replace("(", "( ")
    			.replace(")", " )")
    			.replace("«", "")
    			.replace("»", "");
    }

	private static List<String> collectTree(String xml, boolean onlyNPs) throws XPathExpressionException {
    	InputSource extract = new InputSource(new StringReader(xml));
    	XPathFactory xPathFactory = XPathFactory.newInstance();
    	XPath xPath = xPathFactory.newXPath();

    	XPathExpression tree = null;
    	if (onlyNPs) {
    		tree = xPath.compile("//tree[@cat='np']/t/text() | //tree[@cat='adjp']/t/text()");
    	} else {
        	tree = xPath.compile("//tree//t/text() | //tree//punct/@ort");
    	}

//		XPathExpression tree = xPath.compile("//t/text() | //punct/@ort");
    	NodeList treeResult = (NodeList) tree.evaluate(extract, XPathConstants.NODESET);

    	List<String> values = new ArrayList<>();
    	for (Node node : AppTestBackup.iterable(treeResult)) {
    		if (node.getNodeValue().contains("_")) {
        		values.addAll(Arrays.asList(node.getNodeValue().split("_")));
    		} else
			if (node.getNodeValue().contains("-")) {
				String[] split = node.getNodeValue().split("-");
				values.add(split[0] + "-");
				if (split.length > 1) {
					values.add(split[1]);
				}
			} else {
        		values.add(node.getNodeValue());
    		}
    	}
    	return values;
	}

    private static Iterable<Node> iterable(final NodeList nodeList) {
        return () -> new Iterator<Node>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < nodeList.getLength();
            }

            @Override
            public Node next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return nodeList.item(index++);
            }
        };
    }

    private static List<String[]> replacements() {
    	List<String[]> replacements = new ArrayList<>();

    	replacements.add(new String [] { "a", "a", "à" });
    	replacements.add(new String [] { "a", "as", "às" });
    	replacements.add(new String [] { "a", "o", "ao" });
    	replacements.add(new String [] { "a", "os", "aos" });
    	replacements.add(new String [] { "de", "a", "da" });
    	replacements.add(new String [] { "de", "as", "das" });
    	replacements.add(new String [] { "de", "aquela", "daquela" });
    	replacements.add(new String [] { "de", "aquelas", "daquelas" });
    	replacements.add(new String [] { "de", "aquele", "daquele" });
    	replacements.add(new String [] { "de", "aqueles", "daqueles" });
    	replacements.add(new String [] { "de", "o", "do" });
    	replacements.add(new String [] { "de", "os", "dos" });
    	replacements.add(new String [] { "de", "ela", "dela" });
    	replacements.add(new String [] { "de", "elas", "delas" });
    	replacements.add(new String [] { "de", "ele", "dele" });
    	replacements.add(new String [] { "de", "eles", "deles" });
    	replacements.add(new String [] { "de", "esta", "desta" });
    	replacements.add(new String [] { "de", "estas", "destas" });
    	replacements.add(new String [] { "de", "este", "deste" });
    	replacements.add(new String [] { "de", "estes", "destes" });
    	replacements.add(new String [] { "de", "essa", "dessa" });
    	replacements.add(new String [] { "de", "essas", "dessas" });
    	replacements.add(new String [] { "de", "esse", "desse" });
    	replacements.add(new String [] { "de", "esses", "desses" });
    	replacements.add(new String [] { "em", "a", "na" });
    	replacements.add(new String [] { "em", "as", "nas" });
    	replacements.add(new String [] { "em", "o", "no" });
    	replacements.add(new String [] { "em", "os", "nos" });
    	replacements.add(new String [] { "em", "essa", "nessa" });
    	replacements.add(new String [] { "em", "esse", "nesse" });
    	replacements.add(new String [] { "em", "esta", "nesta" });
    	replacements.add(new String [] { "em", "este", "neste" });
    	replacements.add(new String [] { "em", "um", "num" });
    	replacements.add(new String [] { "em", "uma", "numa" });
    	replacements.add(new String [] { "por", "a", "pela" });
    	replacements.add(new String [] { "por", "as", "pelas" });
    	replacements.add(new String [] { "por", "o", "pelo" });
    	replacements.add(new String [] { "por", "os", "pelos" });
    	
    	return replacements;
    }

}