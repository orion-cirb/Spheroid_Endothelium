import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.ImageProcessorReader;
import org.apache.commons.io.FilenameUtils;


/**
 * @author ORION-CIRB
 */   
public class Spheroid_Endothelium implements PlugIn {
    
    private Spheroid_Endothelium_Tools tools = new Spheroid_Endothelium_Tools();
    private String imageDir;
    private static String outDirResults;
    
    public void run(String arg) {
        try {
            if (!tools.checkInstalledModules()) {
                return;
            }
            
            imageDir = IJ.getDirectory("Choose directory containing image files...");
            if (imageDir == null) {
                return;
            }
            
            // Find images with fileExt extension
            String fileExt = tools.findImageType(new File(imageDir));
            ArrayList<String> imageFiles = tools.findImages(imageDir, fileExt);
            if (imageFiles.isEmpty()) {
                IJ.showMessage("Error", "No images found with " + fileExt + " extension");
                return;
            }
            
            // Create output folder
            outDirResults = imageDir + File.separator + "Results" + File.separator;
            File outDir = new File(outDirResults);
            if (!Files.exists(Paths.get(outDirResults))) {
                outDir.mkdir();
            }
            // Write header in results file
            tools.writeHeaders(outDirResults);
            
            // Create OME-XML metadata store of the latest schema version
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            ImageProcessorReader reader = new ImageProcessorReader();
            reader.setMetadataStore(meta);
            reader.setId(imageFiles.get(0));
            
            // Find image calibration
            tools.findImageCalib(meta);
                 
            for (String f : imageFiles) {
                reader.setId(f);
                String rootName = FilenameUtils.getBaseName(f);
                tools.print("--- ANALYZING IMAGE " + rootName + " ------");
                
                // Find ROIs
                String roiFile = imageDir+rootName+".zip";
                if (!new File(roiFile).exists()) {
                    tools.print("ERROR: No ROI file found for image " + rootName);
                    IJ.showMessage("Error", "No ROI file found for image " + rootName);
                    continue;
                }
                RoiManager rm = new RoiManager(false);
                rm.runCommand("Open", roiFile);
                List<Roi> roisIn = Arrays.asList(rm.getRoisAsArray());
                
                // Order ROIs by slice
                TreeMap<Integer, List<Roi>> rois = new TreeMap<>();
                for (Roi roi : roisIn) {
                    int zPos = roi.getZPosition();
                    if(rois.containsKey(zPos)) {
                        rois.get(zPos).add(roi);
                    } else {
                       rois.put(zPos, new ArrayList<>());
                       rois.get(zPos).add(roi);
                    }
                }
                
                // Open first channel
                ImporterOptions options = new ImporterOptions();
                options.setId(f);
                options.setSplitChannels(true);
                options.setQuiet(true);
                options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                ImagePlus img = BF.openImagePlus(options)[0];
                                
                tools.writeResults(rois, img, rootName, outDirResults);
                tools.closeImage(img);
            }
            
            tools.closeResults();
            tools.print("--- All done! ---");
            
        } catch (DependencyException | ServiceException | IOException | FormatException ex) {
            Logger.getLogger(Spheroid_Endothelium.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

