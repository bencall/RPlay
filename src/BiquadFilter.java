/**
 * Implementation of the biquad filter. Not sure it is correct
 * @author bencall
 *
 */
public class BiquadFilter {
    double bf_playback_rate = 1.0;
    double bf_est_drift = 0.0;   // local clock is slower by
    biquad_t bf_drift_lpf;
    double bf_est_err = 0.0, bf_last_err;
    biquad_t bf_err_lpf, bf_err_deriv_lpf;
    double desired_fill;
    int fill_count;
    int sampling_rate;
    int frame_size;
     
    private static final double CONTROL_A  = (1e-4);
    private static final double CONTROL_B = (1e-1);
     
    public BiquadFilter(int sampling_rate, int frame_size){
        this.sampling_rate = sampling_rate;
        this.frame_size = frame_size;
        bf_drift_lpf = biquad_lpf(1.0/180.0, 0.3);
        bf_err_lpf = biquad_lpf(1.0/10.0, 0.25);
        bf_err_deriv_lpf = biquad_lpf(1.0/2.0, 0.2);
        fill_count = 0;
        bf_playback_rate = 1.0;
        bf_est_err = 0;
        bf_last_err = 0;
        desired_fill = 0;
        fill_count = 0;
    }
     
    private biquad_t biquad_lpf(double freq, double Q) {
        biquad_t ret = new biquad_t();
         
        double w0 = 2*Math.PI*freq/((float)sampling_rate/(float)frame_size);
        double alpha = Math.sin(w0)/(2.0*Q);
         
        double a_0 = 1.0 + alpha;
        ret.b[0] = (1.0-Math.cos(w0))/(2.0*a_0);
        ret.b[1] = (1.0-Math.cos(w0))/a_0;
        ret.b[2] = ret.b[0];
        ret.a[0] = -2.0*Math.cos(w0)/a_0;
        ret.a[1] = (1-alpha)/a_0;
         
        return ret;
    }
     
    public void update(int fill){
        if (fill_count < 1000) {
            desired_fill += (double)fill/1000.0;
            fill_count++;
            return;
        }
                 
        double buf_delta = fill - desired_fill;
        bf_est_err = filter(bf_err_lpf, buf_delta);
        double err_deriv = filter(bf_err_deriv_lpf, bf_est_err - bf_last_err);
             
        bf_est_drift = filter(bf_drift_lpf, CONTROL_B*(bf_est_err*CONTROL_A + err_deriv) + bf_est_drift);
             
        bf_playback_rate = 1.0 + CONTROL_A*bf_est_err + bf_est_drift;    
        bf_last_err = bf_est_err;
    }
     
    private double filter(biquad_t bq, double in) {
        double w = in - bq.a[0]*bq.hist[0] - bq.a[1]*bq.hist[1];
//      double out  = bq.b[1]*bq.hist[0] + bq.b[2]*bq.hist[1] + bq.b[0]*w;
        bq.hist[1] = bq.hist[0];
        bq.hist[0] = w;
        return w;
    }
}