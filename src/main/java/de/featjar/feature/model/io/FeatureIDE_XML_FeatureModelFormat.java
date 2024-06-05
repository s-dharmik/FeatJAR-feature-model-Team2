package de.featjar.feature.model.io;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.featjar.base.data.Result;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.io.format.ParseException;
import de.featjar.base.io.input.AInputMapper;
import de.featjar.feature.model.FeatureModel;
import de.featjar.feature.model.IFeature;
import de.featjar.feature.model.IFeatureModel;
import de.featjar.feature.model.IFeatureTree;

/**
 * @Dharmik
 * Parses and writes feature models from and to FeatureIDE XML files.
 */
public class FeatureIDE_XML_FeatureModelFormat implements IFormat<IFeatureModel> {

	
	
//	This method is used to parse the feature model from the input file.
	@Override
    public Result<IFeatureModel> parse(AInputMapper inputMapper) {
        try {
            String content = inputMapper.get().text();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));
       
            // let's assume that rootElement is the root element of FeatureTree
            Element rootElement = doc.getDocumentElement();
            

            IFeatureModel featureModel = new FeatureModel();
            IFeature rootFeature = createFeature(featureModel, rootElement);
            IFeatureTree tree = featureModel.mutate().addFeatureTreeRoot(rootFeature);

            createFeatureTree(featureModel, rootElement, tree);

            // Parsing constraints if any needed for the feature model
            // NodeList constraints = doc.getElementsByTagName("constraints");

            return Result.of(featureModel);
        } catch (Exception e) {
            return Result.empty(e);
        }
    }

    private IFeature createFeature(IFeatureModel featureModel, Element featureElement) throws ParseException {
        IFeature feature = featureModel.mutate().addFeature(getName(featureElement));
        // Set attributes such as abstract, type, etc.
        
        String isAbstract = featureElement.getAttribute("abstract");
		if (isAbstract != null && !isAbstract.isEmpty()) {
			feature.mutate().setAbstract(Boolean.parseBoolean(isAbstract));
		}
        
        return feature;
    }

    private void createFeatureTree(IFeatureModel featureModel, Element parentElement, Result<IFeatureTree> result) throws ParseException {
        // This code here is parsing the child features.
    	
    	NodeList childNodeList = parentElement.getElementsByTagName("feature");
    	for(int i = 0; i<childNodeList.getLength(); i++) {
    		Element childElement = (Element) childNodeList.item(i);
    		IFeature childFeature = createFeature(featureModel, childElement);
    		result.get().mutate().addChild(childFeature);
    		createFeatureTree(featureModel, childElement, childFeature.getFeatureTree());
    		
    		
    	}
    	
    }

    private String getName(Element featureElement) {
        return featureElement.getAttribute("name");
    }

    // Implement other necessary methods and logic for parsing and creating the feature model tree

    @Override
    public boolean supportsParse() {
        return true;
    }

    @Override
    public boolean supportsSerialize() {
        return false;
    }

    @Override
    public String getFileExtension() {
        return "xml";
    }

    @Override
    public String getName() {
        return "FeatureIDE XML";
    }
}