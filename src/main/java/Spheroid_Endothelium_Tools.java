import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import javax.swing.ImageIcon;
import loci.formats.meta.IMetadata;
import org.apache.commons.io.FilenameUtils;


/**
 * @author ORION-CIRB
 */
public class Spheroid_Endothelium_Tools {
    
    public final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
    
    private Calibration cal;
    private double pixelArea;
    
    private BufferedWriter results;

    
    
    /**
     * Display a message in the ImageJ console and status bar
     */
    public void print(String log) {
        System.out.println(log);
        IJ.showStatus(log);
    }
    
    
    /**
     * Check that needed modules are installed
     */
    public boolean checkInstalledModules() {
        ClassLoader loader = IJ.getClassLoader();
        try {
            loader.loadClass("mcib3d.geom.Object3D");
        } catch (ClassNotFoundException e) {
            IJ.log("3D ImageJ Suite not installed, please install from update site");
            return false;
        }
        return true;
    }
    
    
    /**
     * Flush and close an image
     */
    public void closeImage(ImagePlus img) {
        img.flush();
        img.close();
    }
    
    
    /**
     * Find images extension
     */
    public String findImageType(File imagesFolder) {
        String ext = "";
        String[] files = imagesFolder.list();
        for (String name : files) {
            String fileExt = FilenameUtils.getExtension(name);
            switch (fileExt) {
               case "nd" :
                   ext = fileExt;
                   break;
                case "czi" :
                   ext = fileExt;
                   break;
                case "lif"  :
                    ext = fileExt;
                    break;
                case "ics" :
                    ext = fileExt;
                    break;
                case "ics2" :
                    ext = fileExt;
                    break;
                case "lsm" :
                    ext = fileExt;
                    break;
                case "tif" :
                    ext = fileExt;
                    break;
                case "tiff" :
                    ext = fileExt;
                    break;
            }
        }
        return(ext);
    }
    
    
    /**
     * Find images in folder
     */
    public ArrayList<String> findImages(String imagesFolder, String imageExt) {
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        if (files == null) {
            System.out.println("No image found in " + imagesFolder);
            return null;
        }
        ArrayList<String> images = new ArrayList();
        for (String f : files) {
            // Find images with extension
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equals(imageExt) && !f.startsWith("."))
                images.add(imagesFolder + File.separator + f);
        }
        Collections.sort(images);
        return(images);
    }
       
    
    /**
     * Find image calibration
     */
    public void findImageCalib(IMetadata meta) {
        cal = new Calibration();
        cal.pixelWidth = cal.pixelHeight = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
        cal.pixelDepth = 1;
        cal.setUnit("microns");
        System.out.println("XY calibration = " + cal.pixelWidth);
        
        pixelArea = cal.pixelWidth * cal.pixelHeight;
    }
    
    
    /**
     * Write headers in results files
     */
    public void writeHeaders(String outDirResults) throws IOException {
        // Global results
        FileWriter resultsFile = new FileWriter(outDirResults + "results.xls", false);
        results = new BufferedWriter(resultsFile);
        results.write("Image name\tSlice\tEndothelium area (um2)\tSpheroid area (um2)\tEndothelium area - spheroid area (um2)\t" +
                "Spheroid area / endothelium area\tNb lumens\tLumen name\tLumen area (um2)\tLumen circularity\t"
                + "Lumen aspect ratio\tLumen roundness\tLumen solidity\n");
        results.flush();
    }
    
    
    /**
     * Compute parameters and save them in results files
     * @throws java.io.IOException
     */
    public void writeResults(TreeMap<Integer, List<Roi>> rois, ImagePlus img, String imgName, String outDirResults) throws IOException {
        for(int slice: rois.keySet()) {
            List<Roi> roisInSlice = rois.get(slice);
            
            double cArea = Double.NaN;
            double sArea = Double.NaN;
            for(Roi roi: roisInSlice) {
                if(roi.getName().equals("c"))
                    cArea = getArea(roi, img);
                else if(roi.getName().equals("s"))
                    sArea = getArea(roi, img);
            }
            if (cArea == Double.NaN || sArea == Double.NaN) {
                print("ERROR: ROI(s) c/s missing in slice " + slice + " of image " + imgName);
                IJ.showMessage("ERROR: ROI(s) c/s missing in slice " + slice + " of image " + imgName);
                continue;
            }
            
            double csDiff = cArea - sArea;
            double scRatio = sArea / cArea;
            int nbLumens = roisInSlice.size() - 2;
            if(nbLumens == 0) {
                results.write(imgName+"\t"+slice+"\t"+cArea+"\t"+sArea+"\t"+csDiff+"\t"+scRatio+"\t"+nbLumens+"\n");
            } else {
                for(Roi roi: roisInSlice) {
                    if(!roi.getName().equals("c") && !roi.getName().equals("s")) {
                        ResultsTable rt = getShapeDescriptors(roi, img);
                        results.write(imgName+"\t"+slice+"\t"+cArea+"\t"+sArea+"\t"+csDiff+"\t"+scRatio+"\t"+nbLumens+"\t"+
                                roi.getName()+"\t"+rt.getValue("Area", 0)+"\t"+rt.getValue("Circ.", 0)+"\t"+rt.getValue("AR", 0)+"\t"+
                                rt.getValue("Round", 0)+"\t"+rt.getValue("Solidity", 0)+"\n");
                    }
                }
            }
            

        }
        results.flush();        
    }
    
    
    /**
     * Compute ROI area
     */
    public double getArea(Roi roi, ImagePlus img) {
        img.setCalibration(cal);
        PolygonRoi poly = new PolygonRoi(roi.getFloatPolygon(), Roi.FREEROI);
        poly.setLocation(0, 0);
        img.setRoi(poly);
        
        ResultsTable rt = new ResultsTable();
        Analyzer analyzer = new Analyzer(img, Analyzer.AREA, rt);
        analyzer.measure();
        return(rt.getValue("Area", 0));
    }
    
    /**
     * Compute ROI area
     */
    public ResultsTable getShapeDescriptors(Roi roi, ImagePlus img) {
        img.setCalibration(cal);
        PolygonRoi poly = new PolygonRoi(roi.getFloatPolygon(), Roi.FREEROI);
        poly.setLocation(0, 0);
        img.setRoi(poly);
        
        ResultsTable rt = new ResultsTable();
        Analyzer analyzer = new Analyzer(img, Analyzer.AREA+Analyzer.SHAPE_DESCRIPTORS, rt);
        analyzer.measure();
        return(rt);
    }
    
    /**
     * Close results files
     */
    public void closeResults() throws IOException {
       results.close();
    }
     
}
