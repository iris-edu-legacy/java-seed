package edu.iris.Fissures.seed.util;

import java.lang.*;
import edu.iris.Fissures.seed.container.*;
import edu.iris.Fissures.seed.exception.*;

/**
 * RespFactory file formatting class.
 * @author Chris Laugbon, IRIS DMC<br>
 * ----(original author)<br>
 * Robert Casey, IRIS DMC<br>
 * ----(modifications for MMAP container and SeedTags)
 * @version September, 2009
 *
 */
public class RespFactory {


    public RespFactory(SeedVolumeMMAPContainer c) {
        container = c;
        clear();  // initializes with a new string buffer
    }

    // get the compiled resp results
    public String getResp() {
        return r.toString();
    }

    // set up the station and channel preamble
    public void addStnChnInfo(SeedObjectTag stnTag, SeedObjectTag chnTag) throws ContainerException, SeedException {
        if (container != null) {
            Blockette stnBlk = container.getBlockette(stnTag);
            Blockette chnBlk = container.getBlockette(chnTag);
            addStnChnInfo(stnBlk,chnBlk);
        } else {
            throw new ContainerException("ERROR: container not assigned to RESP class: addStnChnInfo()");
        }
    }

    // private method that accepts blockette params
    private void addStnChnInfo(Blockette stnBlk, Blockette chnBlk) {
        String date;

        r.append("\n#");
        r.append("\n###################################################################################");
        r.append("\n#");
        r.append("\nB050F03     Station:           " + stnBlk.toString(3));
        r.append("\nB050F16     Network:           " + stnBlk.toString(16));
        // REC - if the location value is SPACE SPACE, replace these characters with question marks
        String locOut = chnBlk.toString(3);
        if (locOut.length() == 2 && locOut.equals("  ")) {
            locOut = "??";
        }
        r.append("\nB052F03     Location:          " + locOut);
        // REC -end
        r.append("\nB052F04     Channel:           " + chnBlk.toString(4));

        // format date for no fracs part... pdcc doesn't like them

        date = chnBlk.toString(22);

        if (date.indexOf('.') >= 0) {
            r.append("\nB052F22     Start date:    " + date.substring(0, date.indexOf('.')));
        } else {
            r.append("\nB052F22     Start date:    " + date);
        }

        date = chnBlk.toString(23);

        if (date.indexOf('.') >= 0) {
            r.append("\nB052F23     End date:      " + date.substring(0, date.indexOf('.')));
        } else if (chnBlk.toString(23).length() > 0) {
            r.append("\nB052F23     End date:      " + date);
        } // empty end date -- REC
        else {
            r.append("\nB052F23     End date:      " + "2599,365,23:59:59");
        }
    // REC - end
    }

    private void addHeader(Blockette stnBlk, Blockette chnBlk) {
        String stn = stnBlk.toString(3);
        String loc = chnBlk.toString(3);

        r.append("#                   |        " + stnBlk.toString(16) + "  ");
        if (stn.length() == 5) {
            r.append(stn + " ");
        }
        if (stn.length() == 4) {
            r.append(stn + "  ");
        }
        if (stn.length() == 3) {
            r.append(stn + "   ");
        }
        if (stn.length() == 2) {
            r.append(stn + "    ");
        }
        if (loc.length() == 1) {
            r.append(loc + "  ");
        }
        if (loc.length() == 2) {
            r.append(loc + " ");
        }
        r.append(chnBlk.toString(4) + "           |\n");

        
        try {
            //DBTime stime = new DBTime();
            Btime stime = new Btime(chnBlk.toString(22));
            //stime.setSeedTime(chnBlk.toString(22));

            //DBTime etime = new DBTime();
            Btime etime = null;
            if (chnBlk.toString(23).length() == 0) //etime.setSeedTime("2300,001,00:00:00.0000");
            {
                etime = new Btime("2599,365,23:59:59.9999");
            } else //etime.setSeedTime(chnBlk.toString(23));
            {
                etime = new Btime(chnBlk.toString(23));
            }

            //r.append("#                   |     "+ stime.getRespTime());
            r.append("#                   |     " + stime.toString().substring(0, stime.toString().indexOf('.')));
            //r.append(" to " + etime.getRespTime() + "      |\n");
            r.append(" to " + etime.toString().substring(0, etime.toString().indexOf('.')) + "      |\n");
            r.append("#                   +-----------------------------------+\n");
            r.append("#\n");
        } catch (SeedInputException e) {
            System.err.println("ERROR: improper time format encountered: " + e);
        }

    

    }


    public void addResp(SeedObjectTag stnTag, SeedObjectTag chnTag, SeedObjectTag respTag)
        throws SeedException, ContainerException {

        Blockette stnBlk = null;  // station blockette
        Blockette chnBlk = null;  // channel blockette
        Blockette respBlk = null; // response blockette
        if (container != null) {
            stnBlk = container.getBlockette(stnTag);
            chnBlk = container.getBlockette(chnTag);
            respBlk = container.getBlockette(respTag);
        } else {
            throw new ContainerException("ERROR: container not assigned to RESP class: addResp()");
        }

        // call routines for the individual resp types
        switch (respBlk.getType()) {
            case 53:
                print53(stnBlk, chnBlk, respTag, respBlk);
                break;
            case 54:
                print54(stnBlk, chnBlk, respTag, respBlk);
                break;
            case 55:
                print55(stnBlk, chnBlk, respTag, respBlk);
                break;
            case 57:
                print57(stnBlk, chnBlk, respBlk);
                break;
            case 58:
                print58(stnBlk, chnBlk, respBlk);
                break;
            case 61:
                print61(stnBlk, chnBlk, respTag, respBlk);
                break;
            // case 62:
        }
    }

    
    private void print53(Blockette stnBlk, Blockette chnBlk, SeedObjectTag respTag, Blockette respBlk) {

        int i, ii, fld_cnt;
        int nzs, nps;

        r.append("\n#");

        r.append("\n#                   +-----------------------------------+");
        r.append("\n#                   |     Response (Poles and Zeros)    |\n");

        addHeader(stnBlk, chnBlk);

        r.append("B053F03     Transfer function type:            " + respBlk.toString(3));
        r.append("\nB053F04     Stage sequence number:             " + respBlk.toString(4));

        try {
            SeedObjectTag dictTag = container.getAbbreviation(respTag,5);  // find the abbreviation keyed by field 5
            Blockette dictBlk = container.getBlockette(dictTag);  // get the blockette for the abbreviation
            r.append("\nB053F05     Response in units:          " +
                    //respBlk.getDictionaryObject(5).toString(4) + " - " +
                    //respBlk.getDictionaryObject(5).toString(5));
                    dictBlk.toString(4) + " - " +
                    dictBlk.toString(5));

        } catch (Exception ex) {
            System.err.println("Unable to obtain dictionary reference for Response Blockette 53's input units");
            r.append("\nB053F05     Response in units lookup:          " + respBlk.toString(5) +
                    " - Unknown");
        }

        try {
            SeedObjectTag dictTag = container.getAbbreviation(respTag,6);
            Blockette dictBlk = container.getBlockette(dictTag);
            r.append("\nB053F06     Response out units:         " +
                    dictBlk.toString(4) + " - " +
                    dictBlk.toString(5));
        } catch (Exception ex) {
            System.err.println("Unable to obtain dictionary reference for Response Blockette 53's output units");
            r.append("\nB053F05     Response out units lookup:          " + respBlk.toString(6) +
                    " - Unknown");
        }

        r.append("\nB053F07     AO normalization factor:           " + respBlk.toString(7));

        r.append("\nB053F08     Normalization frequency:           " + respBlk.toString(8));

        nzs = Integer.parseInt(respBlk.toString(9));

        nps = Integer.parseInt(respBlk.toString(14));

        r.append("\nB053F09     Number of zeroes:                  " + nzs);
        r.append("\nB053F14     Number of poles:                   " + nps);

        fld_cnt = 10;

        if (nzs > 0) {
            r.append("\n#              Complex zeroes:");

            r.append("\n#               i  real          imag          real_error    imag_error");

            for (i = 0; i < nzs; i++) {
                r.append("\nB053F10-13      " + i + "  ");

                try {
                    for (ii = 0; ii < 4; ii++) {
                        r.append(respBlk.toString(ii + fld_cnt, i) + " ");
                    }

                } catch (Exception ex) {
                    System.err.println("Zeros exception:" + ex.getMessage());
                    break;
                }

            }

        }

        fld_cnt += 5;    // 4 for the zeros + one for the # poles

        if (nps > 0) {
            r.append("\n#              Complex poles:");

            r.append("\n#               i  real          imag          real_error    imag_error");

            for (i = 0; i < nps; i++) {
                r.append("\nB053F15-18      " + i + "  ");

                try {
                    for (ii = 0; ii < 4; ii++) {
                        r.append(respBlk.toString(ii + fld_cnt, i) + " ");
                    }
                } catch (Exception ex) {
                    System.err.println("Poles exception:" + ex.getMessage());
                    break;
                }

            }

        }


    }

    private void print54(Blockette stnBlk, Blockette chnBlk, SeedObjectTag respTag, Blockette respBlk) {
        int numNumerators;
        int numDenominators;
        int i, ii;
        int fld_cnt;

        numNumerators = Integer.parseInt(respBlk.toString(7));
        numDenominators = Integer.parseInt(respBlk.toString(10));

        r.append("\n#");

        r.append("\n#                   +-----------------------------------+");

        r.append("\n#                   |       Response (Coefficients)     |\n");

        addHeader(stnBlk, chnBlk);

        r.append("B054F03     Transfer function type:            " + respBlk.toString(3));
        r.append("\nB054F04     Stage sequence number:             " + respBlk.toString(4));

        try {
            SeedObjectTag dictTag = container.getAbbreviation(respTag,5);
            Blockette dictBlk = container.getBlockette(dictTag);
            r.append("\nB054F05     Response in units:          " +
                    dictBlk.toString(4) + " - " +
                    dictBlk.toString(5));
        } catch (Exception ex) {
            System.err.println("Unable to obtain dictionary reference for Response Blockette 54's input units");
            r.append("\nB054F05     Response in units lookup:          " + respBlk.toString(5) +
                    " - Unknown");
        }

        try {
            SeedObjectTag dictTag = container.getAbbreviation(respTag,6);
            Blockette dictBlk = container.getBlockette(dictTag);
            r.append("\nB054F06     Response out units:          " +
                    dictBlk.toString(4) + " - " +
                    dictBlk.toString(5));
        } catch (Exception ex) {
            System.err.println("Unable to obtain dictionary reference for Response Blockette 54's input units");
            r.append("\nB054F06     Response out units lookup:          " + respBlk.toString(6) +
                    " - Unknown");
        }

        r.append("\nB054F07     Number of numerators:              " + numNumerators);
        r.append("\nB054F10     Number of denominators:            " + numDenominators);
        r.append("\n#");

        fld_cnt = 8;  // first coeff field

        if (numNumerators > 0) {
            r.append("\n#              Numerator coefficients:");
            r.append("\n#               i  coefficient   error");

            for (i = 0; i < numNumerators; i++) {
                r.append("\nB054F08-09      " + i + "  ");

                try {
                    for (ii = 0; ii < 2; ii++) {
                        r.append(respBlk.toString(ii + fld_cnt, i) + " ");
                    }

                } catch (Exception ex) {
                    System.err.println("Numerators exception:" + ex.getMessage());
                    break;
                }

            }

        }

        fld_cnt = 11;  // first coeff field

        if (numDenominators > 0) {
            r.append("\n#              Denominator coefficients:");
            r.append("\n#               i  coefficient   error");

            for (i = 0; i < numDenominators; i++) {
                r.append("\nB054F11-12      " + i + "  ");

                try {
                    for (ii = 0; ii < 2; ii++) {
                        r.append(respBlk.toString(ii + fld_cnt, i) + " ");
                    }

                } catch (Exception ex) {
                    System.err.println("Denominators exception:" + ex.getMessage());
                    break;
                }

            }

        }

    }

    private void print55(Blockette stnBlk, Blockette chnBlk, SeedObjectTag respTag, Blockette respBlk) {
        int numResps;
        int i, ii;
        int fld_cnt;

        numResps = Integer.parseInt(respBlk.toString(6));

        r.append("\n#");
        r.append("\n#                   +-----------------------------------+");
        r.append("\n#                   |       Response List  	            |\n");

        addHeader(stnBlk, chnBlk);

        r.append("\nB055F03     Stage sequence number:             " + respBlk.toString(3));

        try {
            SeedObjectTag dictTag = container.getAbbreviation(respTag,4);
            Blockette dictBlk = container.getBlockette(dictTag);
            r.append("\nB055F04     Response in units:          " +
                    dictBlk.toString(4) + " - " +
                    dictBlk.toString(5));
        } catch (Exception ex) {
            System.err.println("Unable to obtain dictionary reference for Response Blockette 55's input units");
            r.append("\nB055F04     Response in units lookup:          " + respBlk.toString(4) +
                    " - Unknown");
        }

        try {
            SeedObjectTag dictTag = container.getAbbreviation(respTag,5);
            Blockette dictBlk = container.getBlockette(dictTag);
            r.append("\nB055F05     Response out units:          " +
                    dictBlk.toString(4) + " - " +
                    dictBlk.toString(5));
        } catch (Exception ex) {
            System.err.println("Unable to obtain dictionary reference for Response Blockette 55's input units");
            r.append("\nB055F05     Response out units lookup:          " + respBlk.toString(5) +
                    " - Unknown");
        }

        r.append("\nB055F06     Number of responses listed:        " + numResps);
        r.append("\n#");

        if (numResps > 0) {
            r.append("\n#              i  frequency     amplitude     amplitude err phase angle   phase err");

            fld_cnt = 7;

            for (i = 0; i < numResps; i++) {
                r.append("\nB055F07-11      " + i + "  ");

                try {
                    for (ii = 0; ii < 5; ii++) {
                        r.append(respBlk.toString(ii + fld_cnt, i) + " ");
                    }
                } catch (Exception ex) {
                    System.err.println("Response List exception:" + ex.getMessage());
                    break;
                }
            }
        }
    }

    private void print57(Blockette stnBlk, Blockette chnBlk, Blockette respBlk) {

        r.append("\n#\n");
        r.append("#                   +-----------------------------------+\n");
        r.append("#                   |             Decimation            |\n");
        addHeader(stnBlk, chnBlk);

        r.append("B057F03     Stage sequence number:             " + respBlk.toString(3));
        r.append("\nB057F04     Input sample rate (HZ):            " + respBlk.toString(4));
        r.append("\nB057F05     Decimation factor:                 " + respBlk.toString(5));
        r.append("\nB057F06     Decimation offset:                 " + respBlk.toString(6));
        r.append("\nB057F07     Estimated delay (seconds):         " + respBlk.toString(7));
        r.append("\nB057F08     Correction applied (seconds):      " + respBlk.toString(8));
        r.append("\n#");

    }

    private void print58(Blockette stnBlk, Blockette chnBlk, Blockette respBlk) {
        int stage = Integer.parseInt(respBlk.toString(3));

        r.append("\n#\n");

        if (stage == 0) {
            printStage0(stnBlk, chnBlk, respBlk);
        } else {
            r.append("#                   +-----------------------------------+\n");
            r.append("#                   |             Channel Gain          |\n");

            addHeader(stnBlk, chnBlk);

            r.append("B058F03     Stage sequence number:             " + stage);
            r.append("\nB058F04     Gain:                              " + respBlk.toString(4));
            r.append("\nB058F05     Frequency of gain:                 " + respBlk.toString(5));
            r.append("\nB058F06     Number of calibrations:            0\n");

        }

    }

    private void print61(Blockette stnBlk, Blockette chnBlk, SeedObjectTag respTag, Blockette respBlk) {
        int numCoefficients;
        int i, ii;
        int fld_cnt;

        numCoefficients = Integer.parseInt(respBlk.toString(8));

        r.append("\n#");
        r.append("\n#                   +-----------------------------------+");
        r.append("\n#                   |           FIR Response            |\n");
        addHeader(stnBlk, chnBlk);

        r.append("B061F03     Stage sequence number:             " + respBlk.toString(3));
        r.append("\nB061F04     Response Name:             " + respBlk.toString(4));
        r.append("\nB061F05     Symmetry Code:             " + respBlk.toString(5));
        try {
            SeedObjectTag dictTag = container.getAbbreviation(respTag,6);
            Blockette dictBlk = container.getBlockette(dictTag);
            r.append("\nB061F06     Response in units:          " +
                    dictBlk.toString(4) + " - " +
                    dictBlk.toString(5));
        } catch (Exception ex) {
            System.err.println("Unable to obtain dictionary reference for Response Blockette 61's input units");
            r.append("\nB061F06     Response in units lookup:          " + respBlk.toString(6) + " - Unknown");
        }

        try {
            SeedObjectTag dictTag = container.getAbbreviation(respTag,7);
            Blockette dictBlk = container.getBlockette(dictTag);
            r.append("\nB061F07     Response out units:          " +
                    dictBlk.toString(4) + " - " +
                    dictBlk.toString(5));
        } catch (Exception ex) {
            System.err.println("Unable to obtain dictionary reference for Response Blockette 61's output units");
            r.append("\nB061F07     Response out units lookup:          " + respBlk.toString(7) + " - Unknown");
        }

        r.append("\nB061F08     Number of coefficients:              " + numCoefficients);
        r.append("\n#");

        fld_cnt = 9;

        if (numCoefficients > 0) {
            r.append("\n#              FIR coefficients:");
            r.append("\n#               i  coefficient");

            for (i = 0; i < numCoefficients; i++) {
                try {
                    r.append("\nB061F09      " + i + "  ");
                    r.append(respBlk.toString(fld_cnt, i) + " ");
                } catch (Exception ex) {
                    System.err.println("Coefficients exception:" + ex.getMessage());
                    break;
                }

            }

        }

    }


    private void printStage0(Blockette stnBlk, Blockette chnBlk, Blockette respBlk) {

        r.append("\n#");

        r.append("#                   +-----------------------------------+\n");
        r.append("#                   |      Channel Sensitivity/Gain     |\n");
        addHeader(stnBlk, chnBlk);
        r.append("#\n");
        r.append("B058F03     Stage sequence number:             0");
        r.append("\nB058F04     Sensitivity:                       " + respBlk.toString(4));
        r.append("\nB058F05     Frequency of sensitivity:          " + respBlk.toString(5));
        r.append("\nB058F06     Number of calibrations:            0\n");
    }


    public void clear() {
        r = new StringBuffer();
    }


    StringBuffer r = null;
    SeedVolumeMMAPContainer container = null;
    
}

