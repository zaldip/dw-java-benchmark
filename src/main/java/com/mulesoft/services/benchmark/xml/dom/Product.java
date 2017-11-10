package com.mulesoft.services.benchmark.xml.dom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Callable;
import org.mule.config.i18n.MessageFactory;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class Product implements Callable
{

	@Inject
	private MuleContext muleContext;
	private Double price;
	
	@Value("${xml.dom.product.root-name}")
	String productXMLRoot; 
	
	@Value("${xml.dom.product.node-name}")
	String productXMLnode; 
	
	@Value("${xml.dom.product.errors.price-update}")
	String priceUpdateError; 
	
	@Value("${attributes.product.price}")
	String attrProductPrice; 
	
	@Value("${formats.price.double}")
	String formatPrice; 
	
	public Object updatePrices(InputStream iptStrm) throws MuleException, ParserConfigurationException, SAXException, IOException, TransformerConfigurationException, javax.xml.transform.TransformerException, TransformerFactoryConfigurationError
	{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(iptStrm);
		DecimalFormat dFormat = new DecimalFormat(formatPrice);
		NodeList nList;

		try
		{
			doc.getDocumentElement().normalize();
			if (doc.getDocumentElement().getNodeName() == productXMLRoot)
			{
				nList = doc.getElementsByTagName(productXMLnode);

				for (int temp = 0; temp < nList.getLength(); temp++)
				{
					Node nNode = nList.item(temp);

					if (nNode.getNodeType() == Node.ELEMENT_NODE)
					{
						Element eElement = (Element) nNode;

						price = Double.parseDouble(eElement.getElementsByTagName(attrProductPrice).item(0).getTextContent());
						
						price+=price*0.01;
						
						eElement.getElementsByTagName(attrProductPrice).item(0).setTextContent(dFormat.format(price).toString());
					}
				}
			}
		} 
		catch (Exception ex)
		{
			throw new DefaultMuleException(MessageFactory.createStaticMessage(priceUpdateError), ex);
		}
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Source xmlResult = new DOMSource(doc);
		Result outputTarget = new StreamResult(outputStream);
		TransformerFactory.newInstance().newTransformer().transform(xmlResult, outputTarget);
		InputStream resultIptStrm = new ByteArrayInputStream(outputStream.toByteArray());
		
		return resultIptStrm;
	}

	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception
	{
		return updatePrices((InputStream) eventContext.getMessage().getPayload());
	}
}
