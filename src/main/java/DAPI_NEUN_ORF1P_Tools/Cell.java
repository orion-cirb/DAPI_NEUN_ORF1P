package DAPI_NEUN_ORF1P_Tools;

import java.util.HashMap;
import mcib3d.geom2.Object3DInt;

/**
 * @author ORION-CIRB
 */
public class Cell {
    
    // Nucleus
    private Object3DInt nucleus;
    // Neun cell
    private Object3DInt neun;
    // ORF1p cell
    private Object3DInt orf1p; 
    // Parameters
    public HashMap<String, Double> params;
    

    public Cell(Object3DInt nucleus) {
        this.nucleus = nucleus;
        this.neun = null;
        this.orf1p = null;
        this.params = new HashMap<>();
    }
    
    
    public Object3DInt getNucleus() {
        return nucleus;
    }
    
    public Object3DInt getNeun() {
        return neun;
    }
    
    public Object3DInt getOrf1p() {
        return orf1p;
    }
    
    public void setLabel(double label) {
        params.put("label", label);
    }
    
    public void setNucParams(double dapiBg, double nucVol, double nucIntTot, double nucIntTotCorr) {
        params.put("dapiBg", dapiBg);
        params.put("nucVol", nucVol);
        params.put("nucIntTot", nucIntTot);
        params.put("nucIntTotCorr", nucIntTotCorr);
    }
    
    public void setNeun(Object3DInt neun) {
        this.neun = neun;
    }
    
    public void setNeunParams(double neunBg, Double neunVol, Double neunIntTot, Double neunIntTotCorr) {
        params.put("neunBg", neunBg);
        params.put("neunVol", neunVol);
        params.put("neunIntTot", neunIntTot);
        params.put("neunIntTotCorr", neunIntTotCorr);             
    }
    
    public void setOrf1p(Object3DInt orf1p) {
        this.orf1p = orf1p;
    }
    
    public void setOrf1pParams(double orf1pBg, Double orf1pVol, Double orf1pIntTot, Double orf1pIntTotCorr) {
        params.put("orf1pBg", orf1pBg);
        params.put("orf1pVol", orf1pVol);
        params.put("orf1pIntTot", orf1pIntTot);
        params.put("orf1pIntTotCorr", orf1pIntTotCorr); 
    }
    
    public void setOrf1pFociParams(double orf1pFociNb, double orf1pFociVolTot, double orf1pFociIntTot, double orf1pFociIntTotCorr) {
        params.put("orf1pFociNb", orf1pFociNb);
        params.put("orf1pFociVolTot", orf1pFociVolTot);
        params.put("orf1pFociIntTot", orf1pFociIntTot);
        params.put("orf1pFociIntTotCorr", orf1pFociIntTotCorr);
    }
    
}
