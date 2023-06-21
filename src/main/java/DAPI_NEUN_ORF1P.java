

import DAPI_NEUN_ORF1P_Tools.Cell;
import DAPI_NEUN_ORF1P_Tools.Tools;
import ij.*;
import ij.plugin.PlugIn;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.util.ImageProcessorReader;
import loci.plugins.in.ImporterOptions;
import mcib3d.geom2.Objects3DIntPopulation;
import org.apache.commons.io.FilenameUtils;
import org.scijava.util.ArrayUtils;


/**
 * Detect DAPI nuclei, NeuN cells and ORF1p cells
 * Compute their colocalization
 * Detect ORF1p foci in ORF1p cells
 * @author ORION-CIRB
 */
public class DAPI_NEUN_ORF1P implements PlugIn {
    
    Tools tools = new Tools();
    
    private String imageDir = "";
    public String outDirResults = "";
    public BufferedWriter cellsResults;
    public BufferedWriter globalResults;
   
    
    public void run(String arg) {
        try {
            if ((!tools.checkInstalledModules()) || (!tools.checkStarDistModels())) {
                return;
            } 
            
            imageDir = IJ.getDirectory("Choose directory containing image files...");
            if (imageDir == null) {
                return;
            }   
            // Find images with extension
            String fileExt = tools.findImageType(new File(imageDir));
            ArrayList<String> imageFiles = tools.findImages(imageDir, fileExt);
            if (imageFiles == null) {
                IJ.showMessage("Error", "No images found with " + fileExt + " extension");
                return;
            }
            
            // Create output folder
            outDirResults = imageDir + File.separator+ "Results" + File.separator;
            File outDir = new File(outDirResults);
            if (!Files.exists(Paths.get(outDirResults))) {
                outDir.mkdir();
            }
            
            // Write header in results files
            String header = "Image name\tImage volume\tDAPI bg int mean\tDAPI bg int sd" +
                            "\tNeuN bg int mean\tNeuN bg int sd\tORF1p bg int mean\tORF1p bg int sd" +
                            "\tNb DAPI+\tNb DAPI+ NeuN+\tNb DAPI+ ORF1p+\tNb DAPI+ NeuN- ORF1p-" +
                            "\tNb DAPI+ NeuN+ ORF1p-\tNb DAPI+ NeuN- ORF1p+\tNb DAPI+ NeuN+ ORF1p+\n";
            FileWriter fwGlobalResults = new FileWriter(outDirResults + "globalResults.xls", false);
            globalResults = new BufferedWriter(fwGlobalResults);
            globalResults.write(header);
            globalResults.flush();
            header = "Image name\tNuc label\tNuc area\tNuc circularity\tNuc DAPI int mean\tNuc DAPI int sd\t"+
                     "Nuc NeuN int mean\tNuc NeuN int sd\tNuc ORF1p int mean\tNuc ORF1p int sd\t" +
                     "is NeuN?\tNeuN area\tNeuN int mean\tNeuN int sd\tNeuN ORF1p int mean\tNeuN ORF1p int sd\t" +
                     "is ORF1p?\tORF1p area\tORF1p int mean\tORF1p int sd\tORF1p NeuN int mean\tORF1p NeuN int sd\n";
            FileWriter fwCellsResults = new FileWriter(outDirResults + "cellsResults.xls", false);
            cellsResults = new BufferedWriter(fwCellsResults);
            cellsResults.write(header);
            cellsResults.flush();
            
            // Create OME-XML metadata store of the latest schema version
            ServiceFactory factory;
            factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            ImageProcessorReader reader = new ImageProcessorReader();
            reader.setMetadataStore(meta);
            reader.setId(imageFiles.get(0));
            
            // Find image calibration
            tools.findImageCalib(meta);
            
            // Find channel names
            String[] chsName = tools.findChannels(imageFiles.get(0), meta, reader);
            
            // Channels dialog
            int[] channels = tools.dialog(chsName);
            if (channels == null) {
                IJ.showStatus("Plugin canceled");
                return;
            }

            for (String f : imageFiles) {
                String rootName = FilenameUtils.getBaseName(f);
                tools.print("--- ANALYZING IMAGE " + rootName + " ------");
                reader.setId(f);
                
                ImporterOptions options = new ImporterOptions();
                options.setId(f);
                options.setSplitChannels(true);
                options.setQuiet(true);
                options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                
                // Open DAPI channel
                tools.print("- Analyzing DAPI channel -");
                int indexCh = ArrayUtils.indexOf(chsName, channels[0]);
                ImagePlus imgDAPI = BF.openImagePlus(options)[indexCh];
                // Detect DAPI nuclei with CellPose
                System.out.println("Finding DAPI nuclei...");
                Objects3DIntPopulation dapiPop = tools.cellposeDetection(imgDAPI, true, tools.cellposeNucModel, 1, tools.cellposeNucDiam, tools.cellposeNucStitchThresh, tools.minNucVol, tools.maxNucVol, tools.minNucInt);
                System.out.println(dapiPop.getNbObjects() + " DAPI nuclei found");

                // Open NeuN channel
                tools.print("- Analyzing NeuN channel -");
                indexCh = ArrayUtils.indexOf(chsName, channels[1]);
                ImagePlus imgNeun = BF.openImagePlus(options)[indexCh];
                // Detect NeuN cells with CellPose
                System.out.println("Finding NeuN cells...");
                Objects3DIntPopulation neunPop = tools.cellposeDetection(imgNeun, true, tools.cellposeNeunModel, 1, tools.cellposeNeunDiam, tools.cellposeNeunStitchThresh, tools.minNeunVol, tools.maxNeunVol, tools.minNeunInt);
                System.out.println(neunPop.getNbObjects() + " NeuN cells found");
                
                // Open ORF1p channel
                tools.print("- Analyzing ORF1p channel -");
                indexCh = ArrayUtils.indexOf(chsName, channels[2]);
                ImagePlus imgOrf1p = BF.openImagePlus(options)[indexCh];
                // Detect NeuN cells with CellPose
                System.out.println("Finding ORF1p cells...");
                Objects3DIntPopulation orf1pPop = tools.cellposeDetection(imgOrf1p, true, tools.cellposeOrf1pModel, 1, tools.cellposeOrf1pDiam, tools.cellposeOrf1pStitchThresh, tools.minOrf1pVol, tools.maxOrf1pVol, tools.minOrf1pInt);
                System.out.println(orf1pPop.getNbObjects() + " ORF1p cells found");
               
                tools.print("- Colocalizing nuclei with NeuN and ORF1p cells -");
                ArrayList<Cell> cells = tools.colocalization(dapiPop, neunPop, orf1pPop);
                
                /*tools.print("- Measuring cells parameters -");
                tools.writeCellsParameters(cells, imgDAPI, imgNeun, imgOrf1p);
               
                // Detect ORF1p foci in ORF1p cells
                System.out.println("Finding GFP foci in each nucleus....");
                Objects3DIntPopulation gfpFociPop = tools.stardistFociInCellsPop(imgGFP, cells, "GFP", true);
                
                // Save image objects
                tools.print("- Saving results -");
                tools.drawResults(imgDAPI, imgPV, cells, gfpFociPop, dapiFociPop, rootName, outDirResults);
                
                // Write results
                for (Cell cell : cells) {
                    results.write(rootName+"\t"+cell.params.get("label")+"\t"+cell.params.get("dapiBg")+"\t"+cell.params.get("nucVol")+"\t"+cell.params.get("nucIntTot")+
                        "\t"+cell.params.get("nucIntTotCorr")+"\t"+cell.params.get("gfpBg")+"\t"+cell.params.get("nucGfpIntTot")+"\t"+cell.params.get("nucGfpIntTotCorr")+
                        "\t"+cell.params.get("pvBg")+"\t"+cell.params.get("pvCellVol")+"\t"+cell.params.get("pvCellIntTot")+"\t"+cell.params.get("pvCellIntTotCorr")+
                        "\t"+cell.params.get("pnnBg")+"\t"+cell.params.get("pnnCellVol")+"\t"+cell.params.get("pnnCellIntTot")+"\t"+cell.params.get("pnnCellIntTotCorr")+
                        "\t"+cell.params.get("gfpFociNb")+"\t"+cell.params.get("gfpFociVolTot")+"\t"+cell.params.get("gfpFociIntTot")+
                        "\t"+cell.params.get("gfpFociIntTotCorr")+"\t"+cell.params.get("dapiFociNb")+"\t"+cell.params.get("dapiFociVolTot")+"\t"+cell.params.get("dapiFociIntTot")+
                        "\t"+cell.params.get("dapiFociIntTotCorr")+"\n");
                    results.flush();
                }*/
                
                tools.flush_close(imgDAPI);
                tools.flush_close(imgNeun);
                tools.flush_close(imgOrf1p);
            }
        } catch (IOException | DependencyException | ServiceException | FormatException ex) {
            Logger.getLogger(DAPI_NEUN_ORF1P.class.getName()).log(Level.SEVERE, null, ex);
        }
        tools.print("--- Process done ---");
    }    
}    
