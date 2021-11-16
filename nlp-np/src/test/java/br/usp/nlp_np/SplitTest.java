package br.usp.nlp_np;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

public class SplitTest {

	private static String ROOT_DIR = "C:\\Users\\Paulo.Berlanga\\git-nlp\\";

	private static String XML_CF_PATH = ROOT_DIR + "nlp-np\\src\\test\\xml\\Bosque_CF_8.0.SimTreeML.xml";
	private static String XML_CP_PATH = ROOT_DIR + "nlp-np\\src\\test\\xml\\Bosque_CP_8.0.SimTreeML.xml";

	private static String CONLLU_TRAIN_PATH = ROOT_DIR + "nlp-np\\src\\test\\conllu\\pt_bosque-ud-train.conllu";
	private static String CONLLU_DEV_PATH = ROOT_DIR + "nlp-np\\src\\test\\conllu\\pt_bosque-ud-dev.conllu";
	private static String CONLLU_TEST_PATH = ROOT_DIR + "nlp-np\\src\\test\\conllu\\pt_bosque-ud-test.conllu";

	private static String XML_TRAIN_PATH = ROOT_DIR + "nlp-np\\src\\test\\xml\\train.xml";
	private static String XML_DEV_PATH = ROOT_DIR + "nlp-np\\src\\test\\xml\\dev.xml";
	private static String XML_TEST_PATH = ROOT_DIR + "nlp-np\\src\\test\\xml\\test.xml";

	private List<String> trainIds = new ArrayList<>();
	private List<String> devIds = new ArrayList<>();
	private List<String> testIds = new ArrayList<>();

	@Before
	public void clearFiles() throws IOException {
		File xmlTrainFile = new File(XML_TRAIN_PATH);
    	if (xmlTrainFile.delete()) {
    		xmlTrainFile.createNewFile();
    	}
		File xmlDevFile = new File(XML_DEV_PATH);
    	if (xmlDevFile.delete()) {
    		xmlDevFile.createNewFile();
    	}
		File xmlTestFile = new File(XML_TEST_PATH);
    	if (xmlTestFile.delete()) {
    		xmlTestFile.createNewFile();
    	}
	}

    @Test
    public void processFiles() throws IOException {
    	trainIds = SplitTest.extractIds(CONLLU_TRAIN_PATH);
    	devIds = SplitTest.extractIds(CONLLU_DEV_PATH);
    	testIds = SplitTest.extractIds(CONLLU_TEST_PATH);

		System.out.println("--> Processando cruzamentos para o conjunto de treinamento...");
    	System.out.println("--> Foram encontrados [" + trainIds.size() + "] exemplos em [" + CONLLU_TRAIN_PATH + "]...");
    	SplitTest.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n", XML_TRAIN_PATH);
    	SplitTest.write("<fs>\n", XML_TRAIN_PATH);
    	SplitTest.execute(XML_CF_PATH, XML_TRAIN_PATH, trainIds);
    	SplitTest.execute(XML_CP_PATH, XML_TRAIN_PATH, trainIds);
    	SplitTest.write("</fs>", XML_TRAIN_PATH);
		System.out.println("--> Arquivo de saída gerado com sucesso [" + XML_TRAIN_PATH + "]...");
    	System.out.println("--> Foram perdidos [" + trainIds.size() + "] exemplos: " + trainIds.toString());
		System.out.println("");

		System.out.println("--> Processando cruzamentos para o conjunto de validação...");
    	System.out.println("--> Foram encontrados [" + devIds.size() + "] exemplos em [" + CONLLU_DEV_PATH + "]...");
    	SplitTest.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n", XML_DEV_PATH);
    	SplitTest.write("<fs>\n", XML_DEV_PATH);
    	SplitTest.execute(XML_CF_PATH, XML_DEV_PATH, devIds);
    	SplitTest.execute(XML_CP_PATH, XML_DEV_PATH, devIds);
    	SplitTest.write("</fs>", XML_DEV_PATH);
		System.out.println("--> Arquivo de saída gerado com sucesso [" + XML_DEV_PATH + "]...");
    	System.out.println("--> Foram perdidos [" + devIds.size() + "] exemplos: " + devIds.toString());
		System.out.println("");

		System.out.println("--> Processando cruzamentos para o conjunto de teste...");
    	System.out.println("--> Foram encontrados [" + testIds.size() + "] exemplos em [" + CONLLU_TEST_PATH + "]...");
    	SplitTest.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n", XML_TEST_PATH);
    	SplitTest.write("<fs>\n", XML_TEST_PATH);
    	SplitTest.execute(XML_CF_PATH, XML_TEST_PATH, testIds);
    	SplitTest.execute(XML_CP_PATH, XML_TEST_PATH, testIds);
    	SplitTest.write("</fs>", XML_TEST_PATH);
		System.out.println("--> Arquivo de saída gerado com sucesso [" + XML_TEST_PATH + "]...");
    	System.out.println("--> Foram perdidos [" + testIds.size() + "] exemplos: " + testIds.toString());
		System.out.println("");

    }

    private static List<String> extractIds(String conlluPath) throws IOException {
        File file = new File(conlluPath);
        InputStream inputStream = new FileInputStream(file);
        List<String> ids = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
            	if (line.contains("# sent_id")) {
            		ids.add(line.split("=")[1].trim());
            	}
            }
        }
        return ids;
    }

    private static void execute(String xmlFromPath, String xmlToPath, List<String> ids) throws IOException {

    	try {
            InputStream inputStream = new FileInputStream(xmlFromPath);
            Reader reader = new InputStreamReader(inputStream, "ISO-8859-1");
            InputSource is = new InputSource(reader);
            is.setEncoding("ISO-8859-1");

        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder builder = factory.newDocumentBuilder();
        	Document document = builder.parse(is);
        	XPathFactory xPathFactory = XPathFactory.newInstance();
        	XPath xPath = xPathFactory.newXPath();
        	XPathExpression extract = xPath.compile("/fs/extract");

        	NodeList extractList = (NodeList) extract.evaluate(document, XPathConstants.NODESET);
        	for (Node node : SplitTest.iterable(extractList)) {
        		StringWriter writer = new StringWriter();
        		Transformer transformer = TransformerFactory.newInstance().newTransformer();
        		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        		transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        		transformer.transform(new DOMSource(node), new StreamResult(writer));

        		String sentId = SplitTest.collectSentIdFromSource(writer.toString());
        		if (ids.contains(sentId)) {
            		SplitTest.write(writer.toString(), xmlToPath);
            		ids.remove(sentId);
        		}
        	}

    	} catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }

    private static void write(String content, String xmlToPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xmlToPath, true), StandardCharsets.ISO_8859_1));
        writer.write(content);
        writer.close();    	
    }

    private static String collectSentIdFromSource(String xml) throws XPathExpressionException {
    	InputSource extract = new InputSource(new StringReader(xml));
    	XPathFactory xPathFactory = XPathFactory.newInstance();
    	XPath xPath = xPathFactory.newXPath();

    	XPathExpression source = xPath.compile("//source/text()");
    	String sourceResult = (String) source.evaluate(extract, XPathConstants.STRING);

    	return sourceResult.split("\\s+")[0].trim();
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
