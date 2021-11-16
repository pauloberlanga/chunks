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

public class AnnotationTest {

	private static String ROOT_DIR = "C:\\Users\\Paulo.Berlanga\\git-nlp\\chunks\\";

	private static String XML_TRAIN_PATH = ROOT_DIR + "nlp-np\\src\\test\\xml\\train.xml";
	private static String OUTPUT_TRAIN_PATH = ROOT_DIR + "nlp-np\\src\\test\\txt\\train_annotated.txt";

	private static String XML_DEV_PATH = ROOT_DIR + "nlp-np\\src\\test\\xml\\dev.xml";
	private static String OUTPUT_DEV_PATH = ROOT_DIR + "nlp-np\\src\\test\\txt\\dev_annotated.txt";

	private static String XML_TEST_PATH = ROOT_DIR + "nlp-np\\src\\test\\xml\\test.xml";
	private static String OUTPUT_TEST_PATH = ROOT_DIR + "nlp-np\\src\\test\\txt\\test_annotated.txt";

	private List<String> notProcessedTrainIds = new ArrayList<String>();
	private List<String> notProcessedDevIds = new ArrayList<String>();
	private List<String> notProcessedTestIds = new ArrayList<String>();

	@Before
	public void clearFiles() throws IOException {
		File outputTrainFile = new File(OUTPUT_TRAIN_PATH);
    	if (outputTrainFile.delete()) {
    		outputTrainFile.createNewFile();
    	}
		File outputDevFile = new File(OUTPUT_DEV_PATH);
    	if (outputDevFile.delete()) {
    		outputDevFile.createNewFile();
    	}
		File outputTestFile = new File(OUTPUT_TEST_PATH);
    	if (outputTestFile.delete()) {
    		outputTestFile.createNewFile();
    	}
	}

    @Test
    public void processFiles() {
    	AnnotationTest.execute(XML_TRAIN_PATH, OUTPUT_TRAIN_PATH, notProcessedTrainIds, "treinamento");
    	AnnotationTest.execute(XML_DEV_PATH, OUTPUT_DEV_PATH, notProcessedDevIds, "validação");
    	AnnotationTest.execute(XML_TEST_PATH, OUTPUT_TEST_PATH, notProcessedTestIds, "teste");
    }

    private static void execute(String xmlFilePath, String outputFilePath, List<String> notProcessedIds, String dataset) {
    	try {
    		System.out.println("--> Processando o conjunto de " + dataset + "...");

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

        	int x = 0, y = 0, z = 0;
        	NodeList extractList = (NodeList) extract.evaluate(document, XPathConstants.NODESET);
        	for (Node node : AnnotationTest.iterable(extractList)) {
        		StringWriter writer = new StringWriter();
        		Transformer transformer = TransformerFactory.newInstance().newTransformer();
        		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        		transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        		transformer.transform(new DOMSource(node), new StreamResult(writer));

        		try {
            		if (AnnotationTest.containsChunk(writer.toString(), outputFilePath, notProcessedIds, dataset)) {
                		x++;
            		} else {
                		y++;
            		}
        		} catch (Exception ex) {
        			z++;
        		}
        	}
    		System.out.println("--> Processados [" + String.valueOf(x + y + z) + "] exemplos do arquivo [" + xmlFilePath + "]...");
			System.out.println("--> Foram encontrados chunks em [" + x + "] exemplos...");
			System.out.println("--> Não foram encontrados chunks em [" + y + "] exemplos...");
			System.out.println("--> Foram descartados [" + z + "] exemplos neste processamento: " + notProcessedIds.toString() + "...");
			System.out.println("--> Arquivo anotado gerado com sucesso: [" + outputFilePath + "]...");
			System.out.println("");

    	} catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }

    private static boolean containsChunk(String xml, String outputFilePath, List<String> notProcessedIds, String dataset) throws Exception {    	
    	String[] source = AnnotationTest.collectSourceInArray(xml);
    	String sourceId = source[0].trim() + source[1].trim();

    	try {
        	List<String> treeTokens = AnnotationTest.collectTree(xml, false);
        	List<String> chunkTokens = AnnotationTest.collectTree(xml, true);
    		if (chunkTokens.size() == 0) {
    	        return false;
    		}
        	List<String> annotations = AnnotationTest.annotate(treeTokens, chunkTokens);

    		if (chunkTokens.size() == 0) {
    			AnnotationTest.write(sourceId, "--------------------------------------------------------", outputFilePath);
    			AnnotationTest.write("A", annotations.toString(), outputFilePath);
    			AnnotationTest.write("S", treeTokens.toString(), outputFilePath);
    			AnnotationTest.write("T", chunkTokens.toString(), outputFilePath);
    			return true;
    		} else {
    			AnnotationTest.storeNotProcessed(xml, sourceId, notProcessedIds, dataset);
    			throw new Exception();
    		}
    	} catch (IndexOutOfBoundsException ex) {
			AnnotationTest.storeNotProcessed(xml, sourceId, notProcessedIds, dataset);
			throw new Exception();
    	} catch (Exception e) {
			AnnotationTest.storeNotProcessed(xml, sourceId, notProcessedIds, dataset);
			throw new Exception();
    	}
    }

    private static void storeNotProcessed(String xml, String sourceId, List<String> notProcessedIds, String dataset) throws Exception {
		notProcessedIds.add(sourceId);
//		if (dataset.equals("teste")) {
//			System.out.println("sent_collection.append('" + AnnotationTest.collectSourceInString(xml).replace(sourceId, "").replace("'", "").trim() + "') # " + sourceId);
//		}
    }

    private static void write(String info, String text, String outputFilePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true));
        writer.write(info + " : " + text.replace(", ", " ") + "\n");
        writer.close();
    }

    private static List<String> annotate(List<String> treeTokens, List<String> chunkTokens) throws Exception {
    	List<String> annotations = new ArrayList<>();
    	for (String treeToken : treeTokens) {
    		String annotation = "-";
    		if (chunkTokens.size() > 0 && chunkTokens.get(0).equals(treeToken)) {
        		annotation = "X";
        		chunkTokens.remove(0);
    		}
    		String format = String.join("", Collections.nCopies(treeToken.length() - 1, " "));
    		annotations.add(annotation + format);
    	}
    	return annotations;
    }

    private static String[] collectSourceInArray(String xml) throws XPathExpressionException, Exception {
    	InputSource extract = new InputSource(new StringReader(xml));
    	XPathFactory xPathFactory = XPathFactory.newInstance();
    	XPath xPath = xPathFactory.newXPath();

    	XPathExpression source = xPath.compile("//source/text()");
    	String sourceResult = (String) source.evaluate(extract, XPathConstants.STRING);

    	return AnnotationTest.sourceReps(sourceResult).split("\\s+");
    }

    private static String collectSourceInString(String xml) throws XPathExpressionException {
    	InputSource extract = new InputSource(new StringReader(xml));
    	XPathFactory xPathFactory = XPathFactory.newInstance();
    	XPath xPath = xPathFactory.newXPath();

    	XPathExpression source = xPath.compile("//source/text()");
    	return ((String) source.evaluate(extract, XPathConstants.STRING)).trim();
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

	private static List<String> collectTree(String xml, boolean onlyChunks) throws XPathExpressionException, Exception {
    	InputSource extract = new InputSource(new StringReader(xml));
    	XPathFactory xPathFactory = XPathFactory.newInstance();
    	XPath xPath = xPathFactory.newXPath();

    	XPathExpression tree = null;
    	if (onlyChunks) {
    		tree = xPath.compile("//tree[@cat='np']/t/text() | //tree[@cat='adjp']/t/text()"); // Definir expressão XPath aqui...
    	} else {
        	tree = xPath.compile("//tree//t/text() | //tree//punct/@ort");
    	}

    	NodeList treeResult = (NodeList) tree.evaluate(extract, XPathConstants.NODESET);

    	List<String> values = new ArrayList<>();
    	for (Node node : AnnotationTest.iterable(treeResult)) {
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

}
