/**
 * USNSN.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.iris.dmc.seedcodec;


public class USNSN {

    public static int[] decode(byte[] b,
                               int numSamples,
                               boolean swapBytes,
                               int bias) throws CodecException {
        if (true) {
            throw new CodecException("USNSN decompression is not yet implemented.");
        }
        int error;

        int[] decomp = new int[numSamples];

        byte tmp;
        if (swapBytes) {
            for (int i = 0; i < b.length; i += 4) {
                tmp = b[i];
                b[i] = b[i+3];
                b[i+4] = tmp;
                tmp = b[i+1];
                b[i+1] = b[i+2];
                b[i+2] = tmp;
            }
        }

        IntHolder eod = new IntHolder();
        IntHolder overflow = new IntHolder();
        IntHolder n = new IntHolder(-1);

        error = dcmprs(numSamples,
                       n,
                       decomp,
                       eod,
                       overflow,
                       b);

        return decomp;
    }

    /* ------------------------------------------------------------------------- */
    public static final int NBK = 4096;
    public static final int  NST = 7;

    static boolean prnt = true;
    static int ipt,nct,ifr,iovr,ldcmprs;
    static int npt;

    static int[][] nib = {{4, 4, 4, 6, 6, 8, 8,10,10,12,14,16,20,24,28,32},
        {4, 8,12, 4, 8, 4, 8, 4, 8, 4, 4, 4, 4, 4, 4, 4},
        {2, 4, 6, 3, 6, 4, 8, 5,10, 6, 7, 8,10,12,14,16}};


    public static final int[] mask = {0x00000003,0x0000000F,0x0000003F,0x000000FF,
            0x000003FF,0x00000FFF,0x00003FFF,0x0000FFFF,
            0x0003FFFF,0x000FFFFF,0x003FFFFF,0x00FFFFFF,
            0x03FFFFFF,0x0FFFFFFF,0x3FFFFFFF,0xFFFFFFFF};
    public static final int[] isgn = {0x00000002,0x00000008,0x00000020,0x00000080,
            0x00000200,0x00000800,0x00002000,0x00008000,
            0x00020000,0x00080000,0x00200000,0x00800000,
            0x02000000,0x08000000,0x20000000,0x80000000};
    public static final int[] msgn = {0xFFFFFFFC,0xFFFFFFF0,0xFFFFFFC0,0xFFFFFF00,
            0xFFFFFC00,0xFFFFF000,0xFFFFC000,0xFFFF0000,
            0xFFFC0000,0xFFF00000,0xFFC00000,0xFF000000,
            0xFC000000,0xF0000000,0xC0000000,0x00000000};


    /**
     Dcmprs decompresses a series previously compressed by cmprs and
     cmfin.  On each call to dcmprs, one compression record, provided in
     icmp[], is decompressed into output array idat[max].  On the first
     call, n must be set less than 0.  On successive calls, n will be the
     maintained by dcmprs to be the C array index of the last point
     decompressed (the number of points decompressed into idat so far
     minus one).  Eod will be set to 1 if the entire time series has
     been finished (0 otherwise).  Ovr will be set to 1 if more series
     was available than would fit into idat (0 otherwise).
     */
    static int dcmprs(int maxx, IntHolder n, int[] idat, IntHolder eod, IntHolder ovr, byte[] icmp) throws CodecException {
        int ln,j,lm;
        int ia0 = 0;
        IntHolder fin = new IntHolder();
        IntHolder nn = new IntHolder();

        /* Get the forward integration contstant and the number of samples in the
         record.  Initialize internal variables. */
        ipt = 1;
        ia0 = gnibleOne(icmp,ipt,32,1,1);
        npt = gnibleOne(icmp,ipt,16,1,0);
        ifr = 0;
        ipt = NST;
        nct = ipt-1;
        int id0 = ia0;
        iovr = 0;
        ldcmprs = 1;

        if(n.getVal() < 0) {
            /* If this is the first record, set the first data point to be the
             forward integration constant. */
            n.setVal(0);
            idat[n.getVal()] = ia0;
        }
        else
            /* If this is not the first record, check the internal consistency of
             the new forward integration constant. */
            if(idat[n.getVal()] != ia0) {
                if(prnt)
                    System.err.println("########## ia0 mismatch ########## idat="+idat[n.getVal()]+" ia0="+ia0);
                ldcmprs = 0;
            }
        lm = n.getVal()+npt;

        for(;;) {
            /* Unpack each frame in turn. */
            ifr = ifr+1;
            int[] tmp = unpacknsn(maxx-n.getVal()-1,nn,fin,ovr,eod,icmp, id0);
            System.arraycopy(tmp, 0, idat, n.getVal()+1, tmp.length);
            /* Reset id0 for next time. */
            id0 = tmp[tmp.length-1];
            /* If we were in danger of an integer overflow clean up and get out. */
            if(iovr != 0) {
                ln = (lm <= maxx) ? lm : maxx;
                for(j = n.getVal()+1; j < ln; j++) idat[j] = 0;
                n.setVal(ln-1);
                fin.setVal(1);
            }
            else
                n.setVal( n.getVal()+nn.getVal()+1);
            /* Bail out if the output buffer is full or if this was the last frame. */
            if(maxx-n.getVal() <= 1) ovr.setVal(1);
            if(ovr.getVal() != 0 || fin.getVal() != 0) return(ldcmprs);
        }
    }


    /**
     Subroutine unpacknsn unpacks data out of compression frame ifr into
     array idat[max].  On return, idat will contain n+1 decompressed data
     points.  If the series ended during the compression frame, fin will
     be set to nonzero.  If there was more data in the compression frame
     than will fit into idat, ovr will be set to nonzero.  If this is the
     last frame of the time series, eod will be set to nonzero.
     * id0 is that integration constant or last sample of previous frame.
     */
    static int[] unpacknsn(int maxx, IntHolder n, IntHolder fin, IntHolder ovr, IntHolder eod, byte[] icmp, int id0) throws CodecException {
        int[] key = new int[2];
        int ict,lpt,ian;
        int js,kpt,j,jb,ln;
        int[] idat = new int[maxx];

        /* Initialize output flags. */
        fin.setVal(0);
        ovr.setVal(0);
        eod.setVal(0);

        /* Unpack the frame key fields. */
        gnible(icmp,key,ipt,4,2,2,0);

        /* If the integration constant is over 2**30 or we are using 32-bit
         differences we better bail. */
        if(id0 >= 1073741824 || key[0] >= 15 || key[1] >= 15) {
            if(prnt)
                System.err.println("## impending integer overflow ## id0="+id0+" keys="+key[0]+" "+key[1]+" ipt="+ipt);
            ldcmprs = -1;
            iovr = 1;
            throw new CodecException("impending integer overflow ## id0="+id0+" keys="+key[0]+" "+key[1]+" ipt="+ipt);
        }

        /* Initialize some counters. */
        js = 0;
        kpt = 0;

        /* Loop over the data fields in the frame. */
        for(j=0; j<2; j++) {
            /* Bail out if the output buffer is full. */
            if(js >= maxx) {
                ovr.setVal(1);
                break;
            }
            jb = key[j];
            /* Set the number of samples to unpack to get to the end of the data
             field, the end of the samples in the record, or the end of the
             output buffer, whichever comes first. */
            ln = (nib[1][jb] <= maxx-js) ? nib[1][jb] : maxx-js;
            ln = (ln <= npt) ? ln : npt;
            /* Unpack the data. */
            int[] tmp = new int[ln];
            gnible(icmp,tmp,ipt,nib[0][jb],ln,nib[1][jb],1);
            System.arraycopy(tmp, 0, idat, js, ln);
            /* Update pointers and counters. */
            js = js+ln;
            npt = npt-ln;
            kpt = kpt+ln;
            /* End of the record trap. */
            if(npt <= 0) {
                fin.setVal(1);
                if(j < 1) ipt = ipt+nib[2][0];
                break;
            }
        }

        /* Fiddle the record buffer pointer so that trailer information may be
         found. */
        n.setVal(js-1);

        /* Integrate the first differences to recover the input time series. */
        if(n.getVal() >= 0) {
            idat[0] = idat[0]+id0;
            if(n.getVal() >= 1) {
                for(j=1; j<=n.getVal(); j++)  {
                    /*
                     if (idat[j] == 0)
                     {
                     printf("cnter=%d\n", cnter);
                     }
                     cnter++;
                     */

                    idat[j] = idat[j]+idat[j-1];
                    /* printf("cnter=%d - sample=%d\n", cnter, idat[j]); */

                }

            }
        }


        if(ovr.getVal() != 0 || (ifr%7 != 0 && fin.getVal() ==0)) return idat;

        /* Check the end of block back pointer. */
        nct = ipt-nct;
        ict = gnibleOne(icmp,ipt,8,1,0);
        if(ict != nct) {
            if(prnt)
                System.err.println("########## nct mismatch ########## ict="+ict+" nct="+nct+" ipt="+ipt);
            if(ldcmprs!=-1) ldcmprs = -4;
        }
        nct = ipt-1;
        if(fin.getVal() == 0 || ipt > NBK-4) return idat;
        lpt = gnibleOne(icmp,ipt,8,1,0);
        if(lpt == 0) return idat;

        /* For the last record of the series, check consistency. */
        eod.setVal(1);
        /* Check that the number of samples in the last frame is as expected. */
        if(kpt != lpt) {
            if(prnt)
                System.err.println("########## kpt mismatch ########## kpt="+kpt+" lpt="+lpt+" ipt="+ipt);
            if(ldcmprs>=0) ldcmprs = -5;
        }
        ipt = NBK-3;
        ian = gnibleOne(icmp,ipt,32,1,1);
        /* Check that the reverse integration constant is as expected. */
        if(idat[n.getVal()] != ian) {
            if(prnt)
                System.err.println("########## ian mismatch ########## ian="+ian+" idat[n]="+idat[n.getVal()]);
            if(ldcmprs>=0) ldcmprs = -6;
        }
        return idat;
    }


    /**
     Gnible gets 1 nibble of length nb bits from byte array
     ib beginning at byte ib[ns] and returns it.
     No bits are disturbed in ib.  If sgn != 0, high order bits in ia
     are sign extended from the sign bit of the nibble.  If sgn == 0,
     the nibble is taken to be unsigned and high order bits in ia are
     cleared.  Ns is updated to point to the next unprocessed byte in ib
     assuming that nrun nibbles had been processed (rather than n).  Note
     that even length nibbles up to 32-bits work except for 30-bits.
     */
    static int gnibleOne(byte[] ib, int ns, int nb, int nrun, int sgn) {
        int[] out = new int[1];
        gnible(ib, out, ns, nb, 1, nrun, sgn);
        return out[0];
    }

    /**
     Gnible gets n consecutive nibbles of length nb bits from byte array
     ib beginning at byte ib[ns] and puts them into integer*4 array ia[].
     No bits are disturbed in ib.  If sgn != 0, high order bits in ia
     are sign extended from the sign bit of the nibble.  If sgn == 0,
     the nibble is taken to be unsigned and high order bits in ia are
     cleared.  Ns is updated to point to the next unprocessed byte in ib
     assuming that nrun nibbles had been processed (rather than n).  Note
     that even length nibbles up to 32-bits work except for 30-bits.
     */
    static void gnible(byte[] ib,int[] ia, int ns, int nb, int n, int nrun, int sgn) {
        byte[] ja = new byte[4];
        int ka;
        int kb,isw,mb,mbe,krun,kshf,ishf,ke,npt;
        int k,i,j;

        /* Initialize some constants. */
        ka = 0;
        kb = nb/2-1;
        isw = (kb%4)+1;
        mb = 4-(kb+5-isw)/4;
        npt = ns+(nrun*nb)/8;
        ns -= 1;   /* Bump ns down for the C array indexing convention. */

        switch (isw) {
            case 1:   /* 2, 10, 18, and 26-bit nibbles */

            case 2:   /* 4, 12, 20, and 28-bit nibbles */
                if (isw == 1) {
                    krun = 4;
                } else {
                    krun = 2;
                }
                kshf = 2*isw;
                /* Take the data in groups of krun. */
                for(k = 0; k < n; k = k+krun) {
                    ishf = 8;
                    ke = (k+krun-1<n)?k+krun-1:n-1;

                    /* Unpack each word in this group. */

                    for(i = k; i <= ke; i++) {

                        /* Copy the bytes in this nibble. */

                        ns -= 1;
                        for(j = mb; j <= 3; j++)  {
                            ja[j] = ib[++(ns)];
                        }

                        /* Shift the nibble into place. */

                        ishf = ishf-kshf;
                        ka = ka>>ishf;

                        /* Extend or clear the sign bits as needed. */

                        if((ka&isgn[kb])!=0 & sgn!=0)
                            ia[i] = (ka|msgn[kb]);
                        else
                            ia[i] = (ka&mask[kb]);
                        /*
                         if (ia[i] == 0)
                         printf("found it - cnter=%d\n", cnter);

                         cnter++;
                         */

                    }

                    /* Each group ends on a byte boundary, so adjust ns. */

                    ns += 1;
                }
                break;

            case 3:   /* 6, 14, 22, and 30-bit nibbles */
                kshf = 2*isw;
                /* Take the data in groups of 4. */
                for(k = 0; k < n; k = k+4) {
                    ishf = 8;
                    ke = (k+3<n)?k+3:n-1;
                    /* Unpack each word in this group. */
                    for(i = k; i <= ke; i++) {
                        ishf = ishf-kshf;
                        if(ishf < 0)
                            /* In this case, the second and third words of the group take an extra byte. */
                        {
                            mbe = mb-1;
                            ishf = ishf+8;
                        }
                        else mbe=mb;
                        /* Copy the bytes in this nibble. */
                        ns -= 1;
                        for(j = mbe; j <= 3; j++) ja[j] = ib[++(ns)];
                        /* Shift the nibble into place. */
                        ka = ka>>ishf;
                        /* Extend or clear the sign bits as needed. */

                        if((ka&isgn[kb])!=0 & sgn!=0)
                            ia[i] = (ka|msgn[kb]);
                        else
                            ia[i] = (ka&mask[kb]);
                        /*
                         if (ia[i] == 0)
                         printf("found it cnter=%d\n", cnter);

                         cnter++;
                         */


                    }
                    /* Each group ends on a byte boundary, so adjust ns. */
                    ns += 1;
                }
                break;

            case 4:   /* 8, 16, 24, and 32-bit nibbles */
                ns -= 1;
                /* Loop over each input word. */
                for(i = 0; i < n; i++) {
                    for(j = mb; j <= 3; j++) ja[j] = ib[++(ns)];
                    /* Extend or clear the sign bits as needed. */
                    if((ka&isgn[kb])!=0 & sgn!=0) ia[i] = (ka|msgn[kb]);
                    else                          ia[i] = (ka&mask[kb]);
                    /*
                     if (ia[i] == 0)
                     printf("found it - cnter=%d\n", cnter);

                     cnter++;
                     */


                }
                break;
        }
        /* Adjust ns back to the FORTRAN convention. */
        ns = npt;

        return;
    }


    /**
     Dcmpbr toggles the print flag controlling Dcmprs diagnostic output.
     */
    static void dcmpbr() {
        if(prnt) prnt = false;
        else prnt = true;
        return;
    }

    /**
     Dcmper prints out a Dcmprs diagnostic based on the status flag returned
     by Dcmprs.
     */
    static void dcmper(int ierr) throws CodecException {
        switch (ierr) {
            case  1:                /* Success. */
                break;
            case  0:
                throw new CodecException("IA0 mismatch in Dcmprs.");
            case -1:
                throw new CodecException("Integer overflow in Dcmprs.");
            case -4:
                throw new CodecException("NCT mismatch in Dcmprs.");
            case -5:
                throw new CodecException("KPT mismatch in Dcmprs.");
            case -6:
                throw new CodecException("IAN mismatch in Dcmprs.");
            default:
                throw new CodecException("Unknown error in Dcmprs ("+ierr+").");
        }
        return;
    }

    static class IntHolder {
        public IntHolder(int val) {
            this.val = val;
        }

        public IntHolder() {
            this(0);
        }

        int val;

        public void setVal(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }

}

