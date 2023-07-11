package DAPI_NEUN_ORF1P_Tools;

import java.util.HashMap;
import mcib3d.geom2.Object3DInt;

/**
 * @author ORION-CIRB
 */
public class Cell {
    
    // Nucleus
    public Object3DInt nucleus;
    // Neun cell and cytoplasm
    public Object3DInt neun;
    public Object3DInt neunCyto;
    // ORF1p cell and cytoplasm
    public Object3DInt orf1p;
    public Object3DInt orf1pCyto;
    // Parameters
    public HashMap<String, Double> params;
    

    public Cell(Object3DInt nucleus) {
        this.nucleus = nucleus;
        this.neun = null;
        this.neunCyto = null;
        this.orf1p = null;
        this.orf1pCyto = null;
        this.params = new HashMap<>();
    }
    
    public void setLabel(double label) {
        params.put("label", label);
    }
    
    public void setNucParams(double nucVol, double nucCirc, double nucIntMean, double nucIntSd, 
            double nucIntMeanNeun, double nucIntSdNeun, double nucIntMeanOrf1p, double nucIntSdOrf1p) {
        params.put("nucVol", nucVol);
        params.put("nucCirc", nucCirc);
        params.put("nucIntMean", nucIntMean);
        params.put("nucIntSd", nucIntSd);
        params.put("nucIntMeanNeun", nucIntMeanNeun);
        params.put("nucIntSdNeun", nucIntSdNeun);
        params.put("nucIntMeanOrf1p", nucIntMeanOrf1p);
        params.put("nucIntSdOrf1p", nucIntSdOrf1p);
    }
    
    public void setNeun(Object3DInt neun, Object3DInt neunCyto) {
        this.neun = neun;
        this.neunCyto = neunCyto;
    }
    
    public void setNeunParams(Double neunVol, Double neunIntMean, Double neunIntSd, Double neunIntMeanOrf1p, Double neunIntSdOrf1p,
                              Double neunCytoVol, Double neunCytoIntMean, Double neunCytoIntSd, Double neunCytoIntMeanOrf1p, Double neunCytoIntSdOrf1p) {
        params.put("neunVol", neunVol);
        params.put("neunIntMean", neunIntMean);
        params.put("neunIntSd", neunIntSd);     
        params.put("neunIntMeanOrf1p", neunIntMeanOrf1p);
        params.put("neunIntSdOrf1p", neunIntSdOrf1p);   
        params.put("neunCytoVol", neunCytoVol);
        params.put("neunCytoIntMean", neunCytoIntMean);
        params.put("neunCytoIntSd", neunCytoIntSd);     
        params.put("neunCytoIntMeanOrf1p", neunCytoIntMeanOrf1p);
        params.put("neunCytoIntSdOrf1p", neunCytoIntSdOrf1p);   
    }
    
    public void setOrf1p(Object3DInt orf1p, Object3DInt orf1pCyto) {
        this.orf1p = orf1p;
        this.orf1pCyto = orf1pCyto;
    }
    
    public void setOrf1pParams(Double orf1pVol, Double orf1pIntMean, Double orf1pIntSd, Double orf1pIntMeanNeun, Double orf1pIntSdNeun,
                               Double orf1pCytoVol, Double orf1pCytoIntMean, Double orf1pCytoIntSd, Double orf1pCytoIntMeanNeun, Double orf1pCytoIntSdNeun) {
        params.put("orf1pVol", orf1pVol);
        params.put("orf1pIntMean", orf1pIntMean);
        params.put("orf1pIntSd", orf1pIntSd);
        params.put("orf1pIntMeanNeun", orf1pIntMeanNeun); 
        params.put("orf1pIntSdNeun", orf1pIntSdNeun);
        params.put("orf1pCytoVol", orf1pCytoVol);
        params.put("orf1pCytoIntMean", orf1pCytoIntMean);
        params.put("orf1pCytoIntSd", orf1pCytoIntSd);
        params.put("orf1pCytoIntMeanNeun", orf1pCytoIntMeanNeun); 
        params.put("orf1pCytoIntSdNeun", orf1pCytoIntSdNeun); 
    }
    
}
