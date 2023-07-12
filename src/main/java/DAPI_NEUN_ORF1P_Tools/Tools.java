package DAPI_NEUN_ORF1P_Tools;


import DAPI_NEUN_ORF1P_Tools.Cellpose.CellposeSegmentImgPlusAdvanced;
import DAPI_NEUN_ORF1P_Tools.Cellpose.CellposeTaskSettings;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageStatistics;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.swing.ImageIcon;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.Object3DComputation;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Object3DPlane;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.Objects3DIntPopulationComputation;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.geom2.measurementsPopulation.MeasurePopulationColocalisation;
import mcib3d.image3d.ImageHandler;
import org.apache.commons.io.FilenameUtils;


/**
 * @author ORION-CIRB
 */
public class Tools {
    private final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
    private final String helpUrl = "https://github.com/orion-cirb/DAPI_NEUN_ORF1P";
    
    public String[] channelNames = {"DAPI", "NeuN", "ORF1p"};
    public Calibration cal;
    public double pixelVol;
    
    // Cellpose
    private String cellposeEnvDirPath = IJ.isWindows()? System.getProperty("user.home")+"\\miniconda3\\envs\\CellPose" : "/opt/miniconda3/envs/cellpose";
    // Nuclei detection
    public String cellposeNucModel = "cyto";
    public int cellposeNucDiam = 30;
    public double cellposeNucStitchThresh = 0.75;
    public double minNucVol = 50;
    public double maxNucVol = 1500;
    // NeuN detection
    public String cellposeNeunModel = "cyto2";
    public int cellposeNeunDiam = 40;
    public double cellposeNeunStitchThresh = 0.75;
    public double minNeunVol = 50;
    public double maxNeunVol = 1500;
    // ORF1p detection
    public String cellposeOrf1pModel = "cyto2";
    public int cellposeOrf1pDiam = 40;
    public double cellposeOrf1pStitchThresh = 0.75;
    public double minOrf1pVol = 50;
    public double maxOrf1pVol = 1500;
    
    
    
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
     * Find images extension
     */
    public String findImageType(File imagesFolder) {
        String ext = "";
        File[] files = imagesFolder.listFiles();
        for (File file: files) {
            if(file.isFile()) {
                String fileExt = FilenameUtils.getExtension(file.getName());
                switch (fileExt) {
                    case "nd" :
                       ext = fileExt;
                       break;
                    case "nd2" :
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
                    case "tif" :
                        ext = fileExt;
                        break;
                    case "tiff" :
                        ext = fileExt;
                        break;
                }
            } else if (file.isDirectory() && !file.getName().equals("Results")) {
                ext = findImageType(file);
                if (! ext.equals(""))
                    break;
            }
        }
        return(ext);
    }
    
    
    /**
     * Find images in folder
     */
    public void findImages(String imagesFolder, String imageExt, ArrayList<String> imageFiles) {
        File inDir = new File(imagesFolder);
        File[] files = inDir.listFiles();
        
        for (File file: files) {
            if(file.isFile()) {
                String fileExt = FilenameUtils.getExtension(file.getName());
                if (fileExt.equals(imageExt) && !file.getName().startsWith("."))
                    imageFiles.add(file.getAbsolutePath());
            } else if (file.isDirectory() && !file.getName().contains("Results")) {
                findImages(file.getAbsolutePath(), imageExt, imageFiles);
            }
        }
        Collections.sort(imageFiles);
    }
    
    
    /**
     * Find image calibration
     */
    public void findImageCalib(IMetadata meta) {
        cal = new Calibration();
        cal.pixelWidth = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
        cal.pixelHeight = cal.pixelWidth;
        if (meta.getPixelsPhysicalSizeZ(0) != null)
            cal.pixelDepth = meta.getPixelsPhysicalSizeZ(0).value().doubleValue();
        else
            cal.pixelDepth = 1;
        cal.setUnit("microns");
        System.out.println("XY calibration = " + cal.pixelWidth + ", Z calibration = " + cal.pixelDepth);
    }
    
    
    /**
     * Find channels name
     * @throws loci.common.services.DependencyException
     * @throws loci.common.services.ServiceException
     * @throws loci.formats.FormatException
     * @throws java.io.IOException
     */
    public String[] findChannels (String imageName, IMetadata meta, ImageProcessorReader reader) throws loci.common.services.DependencyException, ServiceException, FormatException, IOException {
        int chs = reader.getSizeC();
        String[] channels = new String[chs];
        String imageExt =  FilenameUtils.getExtension(imageName);
        switch (imageExt) {
            case "nd" :
                for (int n = 0; n < chs; n++) 
                {
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n);
                }
                break;
            case "nd2" :
                for (int n = 0; n < chs; n++) 
                {
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n);
                }
                break;
            case "lif" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null || meta.getChannelName(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n);
                break;
            case "czi" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelFluor(0, n);
                break;
            case "ics" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelExcitationWavelength(0, n).value().toString();
                break;
            case "ics2" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelExcitationWavelength(0, n).value().toString();
                break;   
            default :
                for (int n = 0; n < chs; n++)
                    channels[n] = Integer.toString(n);
        }
        return(channels);         
    }
    
    
    /**
     * Generate dialog box
     */
    public String[] dialog(String[] channels) {
        GenericDialogPlus gd = new GenericDialogPlus("Parameters");
        gd.setInsets​(0, 120, 0);
        gd.addImage(icon);
        
        gd.addMessage("Channels", Font.getFont("Monospace"), Color.blue);
        int index = 0;
        for (String chName: channelNames) {
            gd.addChoice(chName+" : ", channels, channels[index]);
            index++;
        }
        
        gd.addMessage("Nuclei detection", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Min nucleus volume (µm3): ", minNucVol);
        gd.addNumericField("Max nucleus volume (µm3): ", maxNucVol);
    
        gd.addMessage("NeuN detection", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Min cell volume (µm3): ", minNeunVol);
        gd.addNumericField("Max cell volume (µm3): ", maxNeunVol);
        
        gd.addMessage("ORF1p detection", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Min cell volume (µm3): ", minOrf1pVol);
        gd.addNumericField("Max cell volume (µm3): ", maxOrf1pVol);
        
        gd.addMessage("Image calibration", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("XY pixel size (µm): ", cal.pixelWidth);
        gd.addNumericField("Z pixel depth (µm):", cal.pixelDepth);
        gd.addHelp(helpUrl);
        gd.showDialog();
        
        String[] chChoices = new String[channelNames.length];
        for (int n = 0; n < chChoices.length; n++) {
            chChoices[n] = gd.getNextChoice();
        }
        
        minNucVol = gd.getNextNumber();
        maxNucVol = gd.getNextNumber();
        minNeunVol = gd.getNextNumber();
        maxNeunVol = gd.getNextNumber();
        minOrf1pVol = gd.getNextNumber();
        maxOrf1pVol = gd.getNextNumber();
        cal.pixelWidth = cal.pixelHeight = gd.getNextNumber();
        cal.pixelDepth = gd.getNextNumber();
        pixelVol = cal.pixelWidth*cal.pixelWidth*cal.pixelDepth;
        
        if (gd.wasCanceled())
            chChoices = null;
        
        return(chChoices);
    }
    
    
    /**
     * Flush and close an image
     */
    public void closeImg(ImagePlus img) {
        img.flush();
        img.close();
    }
    
    
    /**
     * Look for all 3D cells in a z-stack: 
     * - apply CellPose in 2D slice by slice 
     * - let CellPose reconstruct cells in 3D using the stitch threshold parameter
     */
    public Objects3DIntPopulation cellposeDetection(ImagePlus img, boolean resize, String cellposeModel, int channel, int diameter, double stitchThreshold, double volMin, double volMax) throws IOException{
        ImagePlus imgResized;
        if (resize)
            imgResized = img.resize((int)(img.getWidth()*0.5), (int)(img.getHeight()*0.5), 1, "none");
        else
            imgResized = new Duplicator().run(img);

        // Define CellPose settings
        CellposeTaskSettings settings = new CellposeTaskSettings(cellposeModel, channel, diameter, cellposeEnvDirPath);
        settings.setStitchThreshold(stitchThreshold);
        settings.useGpu(true);
       
        // Run CellPose
        CellposeSegmentImgPlusAdvanced cellpose = new CellposeSegmentImgPlusAdvanced(settings, imgResized);
        ImagePlus imgOut = cellpose.run();
        if(resize) imgOut = imgOut.resize(img.getWidth(), img.getHeight(), "none");
        imgOut.setCalibration(cal);
       
        // Get cells as a population of objects and filter them
        Objects3DIntPopulation pop = new Objects3DIntPopulation(ImageHandler.wrap(imgOut));
        Objects3DIntPopulation popFilter = new Objects3DIntPopulationComputation(pop).getExcludeBorders(ImageHandler.wrap(img), false);
        filterDetectionsByZ(popFilter);
        filterDetectionsBySize(popFilter, volMin, volMax);
        popFilter.resetLabels();
        System.out.println(popFilter.getNbObjects() + " detections remaining after filtering (" + (pop.getNbObjects()-popFilter.getNbObjects()) + " filtered out)");
        
        closeImg(imgResized);
        closeImg(imgOut);
        return(popFilter);
    }
    
    
    /**
     * Remove objects present in only one z slice
     */
    public void filterDetectionsByZ(Objects3DIntPopulation pop) {
        pop.getObjects3DInt().removeIf(p -> (p.getBoundingBox().zmax == p.getBoundingBox().zmin));           
    }
    
    
    /**
     * Remove objects with size < min and size > max
     */
    public void filterDetectionsBySize(Objects3DIntPopulation pop, double min, double max) {
        pop.getObjects3DInt().removeIf(p -> (new MeasureVolume(p).getVolumeUnit() < min) || (new MeasureVolume(p).getVolumeUnit() > max));
    }
        
    
    /**
     * Colocalize nuclei with two different populations of cells
     */
    public ArrayList<Cell> colocalization(Objects3DIntPopulation nucleiPop, Objects3DIntPopulation cellPop1, Objects3DIntPopulation cellPop2) {
        ArrayList<Cell> cells = new ArrayList<Cell>();
        if (nucleiPop.getNbObjects() > 0) {
            MeasurePopulationColocalisation coloc1 = new MeasurePopulationColocalisation(nucleiPop, cellPop1);
            MeasurePopulationColocalisation coloc2 = new MeasurePopulationColocalisation(nucleiPop, cellPop2);
            float label = 1;
            
            for (Object3DInt nucleus: nucleiPop.getObjects3DInt()) {
                Cell cell = new Cell(nucleus);
             
                for (Object3DInt c1: cellPop1.getObjects3DInt()) {
                    double colocVal = coloc1.getValueObjectsPair(nucleus, c1);
                    if (colocVal > 0.5*nucleus.size()) {
                        Object3DInt cyto = new Object3DComputation​(c1).getObjectSubtracted(nucleus);
                        cell.setNeun(c1, cyto);
                        cellPop1.removeObject(c1);
                        break;
                    }
                }
                
                for (Object3DInt c2: cellPop2.getObjects3DInt()) {
                    double colocVal = coloc2.getValueObjectsPair(nucleus, c2);
                    if (colocVal > 0.5*nucleus.size()) {
                        Object3DInt cyto = new Object3DComputation​(c2).getObjectSubtracted(nucleus);
                        cell.setOrf1p(c2, cyto);
                        cellPop2.removeObject(c2);
                        break;
                    }
                }
                
                cell.setLabel(label);
                cells.add(cell);
                label++;
            }
        }
        return(cells);
    }
    
      
    /**
     * Compute images background statistics:
     * Z projection over min intensity + read median/mean/sd intensity
     */
    public HashMap<String, Double> getBackgroundStats(ImagePlus imgDAPI, ImagePlus imgNeun, ImagePlus imgOrf1p) {
        HashMap<String, Double> bgs = new HashMap<>();

        ImageStatistics dapiStats = getZProjectionStats(imgDAPI, ZProjector.MIN_METHOD);
        bgs.put("dapiMedian", dapiStats.median);
        bgs.put("dapiMean", dapiStats.mean);
        bgs.put("dapiSd", dapiStats.stdDev);

        ImageStatistics neunStats = getZProjectionStats(imgNeun, ZProjector.MIN_METHOD);
        bgs.put("neunMedian", neunStats.median);
        bgs.put("neunMean", neunStats.mean);
        bgs.put("neunSd", neunStats.stdDev);

        ImageStatistics orf1pStats = getZProjectionStats(imgOrf1p, ZProjector.MIN_METHOD);
        bgs.put("orf1pMedian", orf1pStats.median);
        bgs.put("orf1pMean", orf1pStats.mean);
        bgs.put("orf1pSd", orf1pStats.stdDev);
        
        return(bgs);
    }
    
    
    /**
     * Do Z projection
     */
    public ImageStatistics getZProjectionStats(ImagePlus img, int param) {
        ZProjector zproject = new ZProjector();
        zproject.setMethod(param);
        zproject.setStartSlice(1);
        zproject.setStopSlice(img.getNSlices());
        zproject.setImage(img);
        zproject.doProjection();
        return(zproject.getProjection().getProcessor().getStatistics());
    }
    

    /**
     * Compute and save PV and PNN cells parameters
     */
    public void writeCellsParameters(ArrayList<Cell> cells, ImagePlus imgDAPI, ImagePlus imgNeun, ImagePlus imgOrf1p, HashMap<String, Double> bgStats) {      
        for (Cell cell : cells) {
            // Nucleus
            Object3DInt nucleus = cell.nucleus;
            double nucVol = new MeasureVolume(nucleus).getVolumeUnit();
            double nucCirc = computeNucleusCircularity(nucleus, imgDAPI);
            double nucIntMean = new MeasureIntensity(nucleus, ImageHandler.wrap(imgDAPI)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG)-bgStats.get("dapiMedian");
            double nucIntSd = new MeasureIntensity(nucleus, ImageHandler.wrap(imgDAPI)).getValueMeasurement(MeasureIntensity.INTENSITY_SD);
            double nucIntMeanNeun = new MeasureIntensity(nucleus, ImageHandler.wrap(imgNeun)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG)-bgStats.get("neunMedian");
            double nucIntSdNeun = new MeasureIntensity(nucleus, ImageHandler.wrap(imgNeun)).getValueMeasurement(MeasureIntensity.INTENSITY_SD);
            double nucIntMeanOrf1p = new MeasureIntensity(nucleus, ImageHandler.wrap(imgOrf1p)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG)-bgStats.get("orf1pMedian");
            double nucIntSdOrf1p = new MeasureIntensity(nucleus, ImageHandler.wrap(imgOrf1p)).getValueMeasurement(MeasureIntensity.INTENSITY_SD);
            cell.setNucParams(nucVol, nucCirc, nucIntMean, nucIntSd, nucIntMeanNeun, nucIntSdNeun, nucIntMeanOrf1p, nucIntSdOrf1p);
           
            // NeuN cell
            Object3DInt neunCell = cell.neun;
            Object3DInt neunCyto = cell.neunCyto;
            if(neunCell != null) {
                double neunVol = new MeasureVolume(neunCell).getVolumeUnit();
                double neunIntMean = new MeasureIntensity(neunCell, ImageHandler.wrap(imgNeun)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG)-bgStats.get("neunMedian");
                double neunIntSd = new MeasureIntensity(neunCell, ImageHandler.wrap(imgNeun)).getValueMeasurement(MeasureIntensity.INTENSITY_SD);
                double neunIntMeanOrf1p = new MeasureIntensity(neunCell, ImageHandler.wrap(imgOrf1p)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG)-bgStats.get("orf1pMedian");
                double neunIntSdOrf1p = new MeasureIntensity(neunCell, ImageHandler.wrap(imgOrf1p)).getValueMeasurement(MeasureIntensity.INTENSITY_SD);
                double neunCytoVol = new MeasureVolume(neunCyto).getVolumeUnit()*pixelVol;
                double neunCytoIntMean = new MeasureIntensity(neunCyto, ImageHandler.wrap(imgNeun)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG)-bgStats.get("neunMedian");
                double neunCytoIntSd = new MeasureIntensity(neunCyto, ImageHandler.wrap(imgNeun)).getValueMeasurement(MeasureIntensity.INTENSITY_SD);
                double neunCytoIntMeanOrf1p = new MeasureIntensity(neunCyto, ImageHandler.wrap(imgOrf1p)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG)-bgStats.get("orf1pMedian");
                double neunCytoIntSdOrf1p = new MeasureIntensity(neunCyto, ImageHandler.wrap(imgOrf1p)).getValueMeasurement(MeasureIntensity.INTENSITY_SD);
                cell.setNeunParams(neunVol, neunIntMean, neunIntSd, neunIntMeanOrf1p, neunIntSdOrf1p, neunCytoVol, neunCytoIntMean, neunCytoIntSd, neunCytoIntMeanOrf1p, neunCytoIntSdOrf1p);
            } else {
                cell.setNeunParams(null, null, null, null, null, null, null, null, null, null);
            }      
            
            // ORF1p cell
            Object3DInt orf1pCell = cell.orf1p;
            Object3DInt orf1pCyto = cell.orf1pCyto;
            if(orf1pCell != null) {
                double orf1pVol = new MeasureVolume(orf1pCell).getVolumeUnit();
                double orf1pIntMean = new MeasureIntensity(orf1pCell, ImageHandler.wrap(imgOrf1p)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG)-bgStats.get("orf1pMedian");
                double orf1pIntSd = new MeasureIntensity(orf1pCell, ImageHandler.wrap(imgOrf1p)).getValueMeasurement(MeasureIntensity.INTENSITY_SD);
                double orf1pIntMeanNeun = new MeasureIntensity(orf1pCell, ImageHandler.wrap(imgNeun)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG)-bgStats.get("neunMedian");
                double orf1pIntSdNeun = new MeasureIntensity(orf1pCell, ImageHandler.wrap(imgNeun)).getValueMeasurement(MeasureIntensity.INTENSITY_SD);
                double orf1pCytoVol = new MeasureVolume(orf1pCyto).getVolumeUnit()*pixelVol;
                double orf1pCytoIntMean = new MeasureIntensity(orf1pCyto, ImageHandler.wrap(imgOrf1p)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG)-bgStats.get("orf1pMedian");
                double orf1pCytoIntSd = new MeasureIntensity(orf1pCyto, ImageHandler.wrap(imgOrf1p)).getValueMeasurement(MeasureIntensity.INTENSITY_SD);
                double orf1pCytoIntMeanNeun = new MeasureIntensity(orf1pCyto, ImageHandler.wrap(imgNeun)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG)-bgStats.get("neunMedian");
                double orf1pCytoIntSdNeun = new MeasureIntensity(orf1pCyto, ImageHandler.wrap(imgNeun)).getValueMeasurement(MeasureIntensity.INTENSITY_SD);
                cell.setOrf1pParams(orf1pVol, orf1pIntMean, orf1pIntSd, orf1pIntMeanNeun, orf1pIntSdNeun, orf1pCytoVol, orf1pCytoIntMean, orf1pCytoIntSd, orf1pCytoIntMeanNeun, orf1pCytoIntSdNeun);
            } else {
                cell.setOrf1pParams(null, null, null, null, null, null, null, null, null, null);
            }
        }
    }
    
    
    public double computeNucleusCircularity(Object3DInt nucleus, ImagePlus img) {
        List<Object3DPlane> planes = nucleus.getObject3DPlanes();
        Object3DPlane middlePlane = planes.get((int)(0.5*((double)planes.size()-1)));
        ImageHandler planeImh = ImageHandler.wrap(IJ.createImage("", "8-bit black", img.getWidth(), img.getHeight(), img.getNSlices()));
        middlePlane.drawObject(planeImh, 255);
        ImagePlus planeImg = planeImh.getImagePlus();
        planeImg.setZ(middlePlane.getZPlane()+1);
        IJ.setAutoThreshold(planeImg, "Default dark no-reset");
        
        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET+ParticleAnalyzer.LIMIT, Measurements.SHAPE_DESCRIPTORS, rt, 0, Double.MAX_VALUE);
        pa.analyze(planeImg​);
        double circ = rt.getValue("Circ.", 0);
        
        closeImg(planeImg);
        return(circ);
    }
    
    
    /**
     * Save detected cells in image
     */
    public void drawResults(ImagePlus img, ArrayList<Cell> cells, String parentFolder, String imageName, String outDir) {
        ImageHandler imgObj1 = ImageHandler.wrap(img).createSameDimensions();
        ImageHandler imgObj2 = imgObj1.createSameDimensions();
        ImageHandler imgObj3 = imgObj1.createSameDimensions();
        
        for(Cell cell: cells) {
            int label = cell.params.get("label").intValue();
            if (cell.neun != null) {
                cell.neunCyto.drawObject(imgObj1, label);
            }
            if (cell.orf1p != null) {
                cell.orf1pCyto.drawObject(imgObj2, label);
            }
            cell.nucleus.drawObject(imgObj3, label);
        }
        
        ImagePlus[] imgColors = {imgObj1.getImagePlus(), imgObj2.getImagePlus(), imgObj3.getImagePlus(), img};
        ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, true);
        imgObjects.setCalibration(cal);
        FileSaver ImgObjectsFile = new FileSaver(imgObjects);
        ImgObjectsFile.saveAsTiff(outDir + parentFolder.replace("/", "_").replace("\\", "_") + imageName + ".tif");
        
        closeImg(imgObj1.getImagePlus());
        closeImg(imgObj2.getImagePlus());
        closeImg(imgObj3.getImagePlus());
        closeImg(imgObjects);
    }
    
    
    public int[] countCells(ArrayList<Cell> cells) {
        int neun = 0, orf1p = 0, noneNone = 0, neunNone = 0, noneOrf1p = 0, neunOrf1p = 0;
        for(Cell cell: cells) {
            if(cell.neun != null) neun++;
            if(cell.orf1p != null) orf1p++;
            if(cell.neun == null && cell.orf1p == null) noneNone++;
            if(cell.neun != null && cell.orf1p == null) neunNone++;
            if(cell.neun == null && cell.orf1p != null) noneOrf1p++;
            if(cell.neun != null && cell.orf1p != null) neunOrf1p++;
        }
        return(new int[]{neun, orf1p, noneNone, neunNone, noneOrf1p, neunOrf1p});
    }
} 
