package DAPI_NEUN_ORF1P_Tools;


import DAPI_NEUN_ORF1P_Tools.Cellpose.CellposeSegmentImgPlusAdvanced;
import DAPI_NEUN_ORF1P_Tools.Cellpose.CellposeTaskSettings;
import DAPI_NEUN_ORF1P_Tools.StardistOrion.StarDist2D;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.ImageIcon;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.BoundingBox;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.Objects3DIntPopulationComputation;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.geom2.measurementsPopulation.MeasurePopulationColocalisation;
import mcib3d.image3d.ImageHandler;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.apache.commons.io.FilenameUtils;
import org.scijava.util.ArrayUtils;


/**
 * @author ORION-CIRB
 */
public class Tools {
    private final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
    private final String helpUrl = "https://github.com/orion-cirb/DAPI_NEUN_ORF1P";
    private CLIJ2 clij2 = CLIJ2.getInstance();
    
    public String[] channelNames = {"DAPI", "NeuN", "ORF1p"};
    public Calibration cal;
    public double pixelVol= 0;
    
    // Cellpose
    private String cellposeEnvDirPath = IJ.isWindows()? System.getProperty("user.home")+"\\miniconda3\\envs\\CellPose" : "/opt/miniconda3/envs/cellpose";
    // Nuclei detection
    public String cellposeNucModel = "cyto";
    public int cellposeNucDiam = 30;
    public double cellposeNucStitchThresh = 0.75;
    public double minNucVol = 0;
    public double maxNucVol = 1000000;
    public double minNucInt = 0;
    // NeuN detection
    public String cellposeNeunModel = "cyto2";
    public int cellposeNeunDiam = 40;
    public double cellposeNeunStitchThresh = 0.75;
    public double minNeunVol = 0;
    public double maxNeunVol = 1000000;
    public double minNeunInt = 0;
    // ORF1p detection
    public String cellposeOrf1pModel = "cyto2";
    public int cellposeOrf1pDiam = 40;
    public double cellposeOrf1pStitchThresh = 0.75;
    public double minOrf1pVol = 0;
    public double maxOrf1pVol = 1000000;
    public double minOrf1pInt = 0;
    
    // StarDist
    private File modelsPath = new File(IJ.getDirectory("imagej")+File.separator+"models");
    private Object syncObject = new Object();
    private String stardistOutput = "Label Image";
    private double stardistPercentileBottom = 0.2;
    private double stardistPercentileTop = 99.8;
    // Foci detection
    private String stardistFociModel = "pmls2.zip";
    private double stardistFociProbThresh = 0.50;
    private double stardistFociOverlayThresh = 0.25;
    public double minFociVol = 0.1;
    public double maxFociVol = 50;
    
    
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
            loader.loadClass("net.haesleinhuepf.clij2.CLIJ2");
        } catch (ClassNotFoundException e) {
            IJ.log("CLIJ not installed, please install from update site");
            return false;
        }
        try {
            loader.loadClass("mcib3d.geom.Object3D");
        } catch (ClassNotFoundException e) {
            IJ.log("3D ImageJ Suite not installed, please install from update site");
            return false;
        }
        return true;
    }
    
    
    /**
     * Check that required StarDist models are present in Fiji models folder
     */
    public boolean checkStarDistModels() {
        FilenameFilter filter = (dir, name) -> name.endsWith(".zip");
        File[] modelList = modelsPath.listFiles(filter);
        int index = ArrayUtils.indexOf(modelList, new File(modelsPath+File.separator+stardistFociModel));
        if (index == -1) {
            IJ.showMessage("Error", stardistFociModel + " StarDist model not found, please add it in Fiji models folder");
            return false;
        }
        return true;
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
    public int[] dialog(String[] channels) {
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
        
        gd.addMessage("Foci detection", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("StarDist probability threshold", stardistFociProbThresh);
        gd.addNumericField("Min foci volume (µm3): ", minFociVol);
        gd.addNumericField("Max foci volume (µm3): ", maxFociVol);
        
        gd.addMessage("Image calibration", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("XY pixel size (µm): ", cal.pixelWidth);
        gd.addNumericField("Z pixel depth (µm):", cal.pixelDepth);
        gd.addHelp(helpUrl);
        gd.showDialog();
        
        int[] chChoices = new int[channelNames.length];
        for (int n = 0; n < chChoices.length; n++) {
            chChoices[n] = ArrayUtils.indexOf(channels, gd.getNextChoice());
        }
        
        minNucVol = gd.getNextNumber();
        maxNucVol = gd.getNextNumber();
        minNeunVol = gd.getNextNumber();
        maxNeunVol = gd.getNextNumber();
        minOrf1pVol = gd.getNextNumber();
        maxOrf1pVol = gd.getNextNumber();
        stardistFociProbThresh = gd.getNextNumber();
        minFociVol = gd.getNextNumber();
        maxFociVol = gd.getNextNumber();
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
    public void flush_close(ImagePlus img) {
        img.flush();
        img.close();
    }
    
    
    /**
     * Look for all 3D cells in a z-stack: 
     * - apply CellPose in 2D slice by slice 
     * - let CellPose reconstruct cells in 3D using the stitch threshold parameter
     */
    public Objects3DIntPopulation cellposeDetection(ImagePlus img, boolean resize, String cellposeModel, int channel, int diameter, double stitchThreshold, double volMin, double volMax, double intMin) throws IOException{
        ImagePlus imgResized;
        if (resize) {
            float resizeFactor = 0.5f;
            imgResized = img.resize((int)(img.getWidth()*resizeFactor), (int)(img.getHeight()*resizeFactor), 1, "none");
        } else {
            imgResized = new Duplicator().run(img);
        }

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
        ImageHandler imgH = ImageHandler.wrap(imgOut);
        Objects3DIntPopulation pop = new Objects3DIntPopulation(imgH);
        Objects3DIntPopulation popFilter = new Objects3DIntPopulationComputation(pop).getExcludeBorders(ImageHandler.wrap(img), false);
        popFilter = new Objects3DIntPopulationComputation​(popFilter).getFilterSize​(volMin/pixelVol, volMax/pixelVol);
        filterDetectionsByIntensity(popFilter, img, intMin);
        filterDetectionsByZ(popFilter);
        popFilter.resetLabels();
        System.out.println(popFilter.getNbObjects() + " detections remaining after filtering (" + (pop.getNbObjects()-popFilter.getNbObjects()) + " filtered out)");
               
        flush_close(imgOut);
        imgH.closeImagePlus();
        return(popFilter);
    }
    
    
    /**
     * Remove objects present in only one z slice
     */
    public void filterDetectionsByZ(Objects3DIntPopulation pop) {
        pop.getObjects3DInt().removeIf(p -> (p.getBoundingBox().zmax == p.getBoundingBox().zmin));           
    }
    
   
    /**
     * Remove objects if intensity mean is less than a certain threshold
     */
    public void filterDetectionsByIntensity(Objects3DIntPopulation pop, ImagePlus img, double intMin) {
        ImageHandler imh = ImageHandler.wrap(img);
        pop.getObjects3DInt().removeIf(p -> (new MeasureIntensity(p, imh).getValueMeasurement(MeasureIntensity.INTENSITY_AVG) <= intMin)); 
    }
        
    
    /**
     * Ccolocalize nuclei with two different populations of cells
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
                        cell.setNeun(c1);
                        cellPop1.removeObject(c1);
                        break;
                    }
                }
                
                for (Object3DInt c2: cellPop2.getObjects3DInt()) {
                    double colocVal = coloc2.getValueObjectsPair(nucleus, c2);
                    if (colocVal > 0.5*nucleus.size()) {
                        cell.setOrf1p(c2);
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
     * Compute and save PV and PNN cells parameters
     */
    public void writeCellsParameters(ArrayList<Cell> cells, ImagePlus imgDAPI, ImagePlus imgPV, ImagePlus imgPNN, ImagePlus imgGFP) {
        double dapiBg = findBackground(imgDAPI, "DAPI");
        double pvBg = findBackground(imgPV, "PV");
        double pnnBg = findBackground(imgPNN, "PNN");
        
        for (Cell cell : cells) {
            float label = cell.params.get("label").floatValue();
            
            // Nucleus
            Object3DInt nucleus = cell.getNucleus();
            nucleus.setLabel(label);
            double nucVol = new MeasureVolume(nucleus).getVolumeUnit();
            double nucIntTot = new MeasureIntensity(nucleus, ImageHandler.wrap(imgDAPI)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
            double nucGFPIntTot = new MeasureIntensity(nucleus, ImageHandler.wrap(imgGFP)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
            cell.setNucParams(dapiBg, nucVol, nucIntTot, nucIntTot-dapiBg*nucVol);

            // PV cell
            Object3DInt pvCell = cell.getNeun();
            if(pvCell != null) {
                pvCell.setLabel(label);
                double pvCellVol = new MeasureVolume(pvCell).getVolumeUnit();
                double pvCellIntTot = new MeasureIntensity(pvCell, ImageHandler.wrap(imgPV)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
                cell.setNeunParams(pvBg, pvCellVol, pvCellIntTot, pvCellIntTot-pvBg*pvCellVol);
            } else {
                cell.setNeunParams(pvBg, null, null, null);
            }
            
            // PNN cell
            Object3DInt pnnCell = cell.getOrf1p();
            if(pnnCell != null) {
                pnnCell.setLabel(label);
                double pnnCellVol = new MeasureVolume(pnnCell).getVolumeUnit();
                double pnnCellIntTot = new MeasureIntensity(pnnCell, ImageHandler.wrap(imgPNN)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
                cell.setOrf1pParams(pnnBg, pnnCellVol, pnnCellIntTot, pnnCellIntTot-pnnBg*pnnCellVol);
            } else {
                cell.setOrf1pParams(pnnBg, null, null, null);
            }
        }
    }
    
    
    /**
     * Do Z projection
     * @param img
     * @param projection parameter
     */
    public ImagePlus doZProjection(ImagePlus img, int param) {
        ZProjector zproject = new ZProjector();
        zproject.setMethod(param);
        zproject.setStartSlice(1);
        zproject.setStopSlice(img.getNSlices());
        zproject.setImage(img);
        zproject.doProjection();
       return(zproject.getProjection());
    }

    /**
     * Find background image intensity:
     * Z projection over min intensity + read median intensity
     * @param img
     */
    public double findBackground(ImagePlus img, String channelName) {
      ImagePlus imgProj = doZProjection(img, ZProjector.MIN_METHOD);
      ImageProcessor imp = imgProj.getProcessor();
      double bg = imp.getStatistics().median;
      System.out.println(channelName + " background (median of the min projection) = " + bg);
      flush_close(imgProj);
      return(bg);
    }
    
    
    /** 
    * For each nucleus find foci
    * return foci pop cell population
    */
    public Objects3DIntPopulation stardistFociInCellsPop(ImagePlus img, ArrayList<Cell> cells, String fociType, boolean resize) throws IOException{
        float fociIndex = 1;
        Objects3DIntPopulation allFociPop = new Objects3DIntPopulation();
        for (Cell cell: cells) {
            Object3DInt nuc = cell.getNucleus();
            
            // Crop image around nucleus
            BoundingBox box = nuc.getBoundingBox();
            Roi roiBox = new Roi(box.xmin, box.ymin, box.xmax-box.xmin, box.ymax-box.ymin);
            img.setRoi(roiBox);
            img.updateAndDraw();
            ImagePlus imgNuc = new Duplicator().run(img, box.zmin+1, box.zmax+1);
            imgNuc.deleteRoi();
            imgNuc.updateAndDraw();
            
            // Downscaling and median filter
            ImagePlus imgS = imgNuc.duplicate();
            if (resize) imgS  = imgS.resize((int)(0.25*imgNuc.getWidth()), (int)(0.25*imgNuc.getHeight()), 1, "none");
            ImagePlus imgM = median_filter(imgS, 1, 1);
            flush_close(imgS);

            // StarDist
            File starDistModelFile = new File(modelsPath+File.separator+stardistFociModel);
            StarDist2D star = new StarDist2D(syncObject, starDistModelFile);
            star.loadInput(imgM);
            star.setParams(stardistPercentileBottom, stardistPercentileTop, stardistFociProbThresh, stardistFociOverlayThresh, stardistOutput);
            star.run();
            flush_close(imgM);

            // Label foci in 3D
            ImagePlus imgLabels = star.associateLabels();
            if (resize) imgLabels = imgLabels.resize(imgNuc.getWidth(), imgNuc.getHeight(), 1, "none");
            
            flush_close(imgNuc);
            imgLabels.setCalibration(cal);
            Objects3DIntPopulation fociPop = new Objects3DIntPopulation(ImageHandler.wrap(imgLabels));
            fociPop = new Objects3DIntPopulationComputation(fociPop).getFilterSize(minFociVol/pixelVol, maxFociVol/pixelVol);
            filterDetectionsByZ(fociPop);
            fociPop.resetLabels();
            flush_close(imgLabels);
            
            // Find foci in nucleus
            fociPop.translateObjects(box.xmin, box.ymin, box.zmin);
            Objects3DIntPopulation fociColocPop = findFociInCell(nuc, fociPop);
            System.out.println(fociColocPop.getNbObjects() + " " + fociType + " foci found in nucleus " + nuc.getLabel());
            
            for (Object3DInt foci: fociColocPop.getObjects3DInt()) {
                foci.setLabel(fociIndex);
                fociIndex++;
                foci.setType(cell.params.get("label").intValue());
                allFociPop.addObject(foci);
            }
            
            writeFociParameters(cell, fociColocPop, img, fociType);
        }
        return(allFociPop);
    }
    
    
    /**
     * Median filter using CLIJ2
     */ 
    public ImagePlus median_filter(ImagePlus img, double sizeXY, double sizeZ) {
       ClearCLBuffer imgCL = clij2.push(img);
       ClearCLBuffer imgCLMed = clij2.create(imgCL);
       clij2.median3DBox(imgCL, imgCLMed, sizeXY, sizeXY, sizeZ);
       clij2.release(imgCL);
       ImagePlus imgMed = clij2.pull(imgCLMed);
       clij2.release(imgCLMed);
       return(imgMed);
    }
    
   
    /**
     * Find dots population colocalizing with a cell objet
     */
    public Objects3DIntPopulation findFociInCell(Object3DInt cellObj, Objects3DIntPopulation dotsPop) {
        Objects3DIntPopulation cellPop = new Objects3DIntPopulation();
        cellPop.addObject(cellObj);
        Objects3DIntPopulation colocPop = new Objects3DIntPopulation();
        if (dotsPop.getNbObjects() > 0) {
            MeasurePopulationColocalisation coloc = new MeasurePopulationColocalisation(cellPop, dotsPop);
            for (Object3DInt dot: dotsPop.getObjects3DInt()) {
                    double colocVal = coloc.getValueObjectsPair(cellObj, dot);
                    if (colocVal > 0.75*dot.size()) {
                        colocPop.addObject(dot);
                    }
            }
        }
        return(colocPop);
    }

    
    /**
     * Compute foci parameters and save them in corresponding Nucleus
     */
    public void writeFociParameters(Cell cell, Objects3DIntPopulation fociColocPop, ImagePlus img, String fociType) {
        int fociNb = fociColocPop.getNbObjects();
        double fociVol = 0;
        double fociInt = 0;
        ImageHandler imh = ImageHandler.wrap(img.duplicate());
        for (Object3DInt obj: fociColocPop.getObjects3DInt()) {
            fociVol += new MeasureVolume(obj).getVolumeUnit();
            fociInt += new MeasureIntensity(obj, imh).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
        }
        

        cell.setOrf1pFociParams(fociNb, fociVol, fociInt, fociInt-cell.params.get("dapiBg")*fociVol/pixelVol);
    }
    
    /**
     * Label object
     * @param popObj
     * @param img 
     * @param fontSize 
     */
    public void labelObject(Object3DInt obj, ImagePlus img, int fontSize) {
        BoundingBox bb = obj.getBoundingBox();
        int z = bb.zmin;
        int x = bb.xmin;
        int y = bb.ymin;
        img.setSlice(z+1);
        ImageProcessor ip = img.getProcessor();
        ip.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        ip.setColor(255);
        ip.drawString(Integer.toString((int)obj.getLabel()), x, y);
        img.updateAndDraw();
    }
   
    
    /**
     * Save detected cells and foci in image
     */
    public void drawResults(ImagePlus imgDAPI, ImagePlus imgPV, ArrayList<Cell> cells, Objects3DIntPopulation gfpFociPop, 
            Objects3DIntPopulation dapiFociPop, String imageName, String outDir) {

        ImageHandler imgObj1 = ImageHandler.wrap(imgDAPI).createSameDimensions();
        ImageHandler imgObj2 = imgObj1.createSameDimensions();
        ImageHandler imgObj3 = imgObj1.createSameDimensions();
        
        for(Cell cell: cells) {
            boolean isPV = false;
            if (cell.getNeun() != null) {
                cell.getNeun().drawObject(imgObj1);
                cell.getNucleus().drawObject(imgObj1, 0);
                isPV = true;
            }
            if (cell.getOrf1p() != null) {
                cell.getOrf1p().drawObject(imgObj2);
                cell.getNucleus().drawObject(imgObj2, 0);
            }
            cell.getNucleus().drawObject(imgObj3);
            
            if (isPV)
                labelObject(cell.getNeun(), imgObj3.getImagePlus(), 50);
            else
                labelObject(cell.getOrf1p(), imgObj3.getImagePlus(), 50);
        }
        
        ImagePlus[] imgColors1 = {imgObj1.getImagePlus(), imgObj2.getImagePlus(), imgObj3.getImagePlus(), imgPV};
        ImagePlus imgObjects1 = new RGBStackMerge().mergeHyperstacks(imgColors1, true);
        imgObjects1.setCalibration(imgDAPI.getCalibration());
        FileSaver ImgObjectsFile1 = new FileSaver(imgObjects1);
        ImgObjectsFile1.saveAsTiff(outDir + imageName + "_cells.tif");
        flush_close(imgObjects1);
        
        ImageHandler imgObj4 = imgObj1.createSameDimensions();
        ImageHandler imgObj5 = imgObj1.createSameDimensions();
        for (Object3DInt dot: gfpFociPop.getObjects3DInt()) {
            dot.drawObject(imgObj4, 255);
            dot.drawObject(imgObj3, 0);
        }
        for (Object3DInt dot: dapiFociPop.getObjects3DInt()) {
            dot.drawObject(imgObj5, 255);
            dot.drawObject(imgObj3, 0);
        }
       
        ImagePlus[] imgColors2 = {imgObj5.getImagePlus(), imgObj4.getImagePlus(), imgObj3.getImagePlus(), imgDAPI};
        ImagePlus imgObjects2 = new RGBStackMerge().mergeHyperstacks(imgColors2, true);
        imgObjects2.setCalibration(imgDAPI.getCalibration());
        FileSaver ImgObjectsFile2 = new FileSaver(imgObjects2);
        ImgObjectsFile2.saveAsTiff(outDir + imageName + "_foci.tif");
        flush_close(imgObjects2);
        
        imgObj1.closeImagePlus();
        imgObj2.closeImagePlus();
        imgObj3.closeImagePlus();
        imgObj4.closeImagePlus();
        imgObj5.closeImagePlus();
    }
} 
