var pi = 3.14159265358979; 
/* Ellipsoid model constants (actual values here are for WGS84) */
var sm_a = 6378137.0;
var sm_b = 6356752.314;
var sm_EccSquared = 6.69437999013e-03;
var UTMScaleFactor = 0.9996;
 
 
/*
* Converts degrees to radians.
*/
function DegToRad (deg)
{
	return (deg / 180.0 * pi)
}

/*
* Converts radians to degrees.
*/
function RadToDeg (rad)
{
	return (rad / pi * 180.0)
}

function UTMCentralMeridian (zone)
{
	var cmeridian;

	cmeridian = DegToRad (-183.0 + (zone * 6.0));

	return cmeridian;
}


function FootpointLatitude (y)
{
	var y_, alpha_, beta_, gamma_, delta_, epsilon_, n;
	var result;
	
	/* Precalculate n (Eq. 10.18) */
	n = (sm_a - sm_b) / (sm_a + sm_b);
		
	/* Precalculate alpha_ (Eq. 10.22) */
	/* (Same as alpha in Eq. 10.17) */
	alpha_ = ((sm_a + sm_b) / 2.0)
		* (1 + (Math.pow (n, 2.0) / 4) + (Math.pow (n, 4.0) / 64));
	
	/* Precalculate y_ (Eq. 10.23) */
	y_ = y / alpha_;
	
	/* Precalculate beta_ (Eq. 10.22) */
	beta_ = (3.0 * n / 2.0) + (-27.0 * Math.pow (n, 3.0) / 32.0)
		+ (269.0 * Math.pow (n, 5.0) / 512.0);
	
	/* Precalculate gamma_ (Eq. 10.22) */
	gamma_ = (21.0 * Math.pow (n, 2.0) / 16.0)
		+ (-55.0 * Math.pow (n, 4.0) / 32.0);
		
	/* Precalculate delta_ (Eq. 10.22) */
	delta_ = (151.0 * Math.pow (n, 3.0) / 96.0)
		+ (-417.0 * Math.pow (n, 5.0) / 128.0);
		
	/* Precalculate epsilon_ (Eq. 10.22) */
	epsilon_ = (1097.0 * Math.pow (n, 4.0) / 512.0);
		
	/* Now calculate the sum of the series (Eq. 10.21) */
	result = y_ + (beta_ * Math.sin (2.0 * y_))
		+ (gamma_ * Math.sin (4.0 * y_))
		+ (delta_ * Math.sin (6.0 * y_))
		+ (epsilon_ * Math.sin (8.0 * y_));
	
	return result;
}


function MapXYToLatLon (x, y, lambda0, philambda)
{
	var phif, Nf, Nfpow, nuf2, ep2, tf, tf2, tf4, cf;
	var x1frac, x2frac, x3frac, x4frac, x5frac, x6frac, x7frac, x8frac;
	var x2poly, x3poly, x4poly, x5poly, x6poly, x7poly, x8poly;
	
	/* Get the value of phif, the footpoint latitude. */
	phif = FootpointLatitude (y);
		
	/* Precalculate ep2 */
	ep2 = (Math.pow (sm_a, 2.0) - Math.pow (sm_b, 2.0))
		  / Math.pow (sm_b, 2.0);
		
	/* Precalculate cos (phif) */
	cf = Math.cos (phif);
		
	/* Precalculate nuf2 */
	nuf2 = ep2 * Math.pow (cf, 2.0);
		
	/* Precalculate Nf and initialize Nfpow */
	Nf = Math.pow (sm_a, 2.0) / (sm_b * Math.sqrt (1 + nuf2));
	Nfpow = Nf;
		
	/* Precalculate tf */
	tf = Math.tan (phif);
	tf2 = tf * tf;
	tf4 = tf2 * tf2;
	
	/* Precalculate fractional coefficients for x**n in the equations
	   below to simplify the expressions for latitude and longitude. */
	x1frac = 1.0 / (Nfpow * cf);
	
	Nfpow *= Nf;   /* now equals Nf**2) */
	x2frac = tf / (2.0 * Nfpow);
	
	Nfpow *= Nf;   /* now equals Nf**3) */
	x3frac = 1.0 / (6.0 * Nfpow * cf);
	
	Nfpow *= Nf;   /* now equals Nf**4) */
	x4frac = tf / (24.0 * Nfpow);
	
	Nfpow *= Nf;   /* now equals Nf**5) */
	x5frac = 1.0 / (120.0 * Nfpow * cf);
	
	Nfpow *= Nf;   /* now equals Nf**6) */
	x6frac = tf / (720.0 * Nfpow);
	
	Nfpow *= Nf;   /* now equals Nf**7) */
	x7frac = 1.0 / (5040.0 * Nfpow * cf);
	
	Nfpow *= Nf;   /* now equals Nf**8) */
	x8frac = tf / (40320.0 * Nfpow);
	
	/* Precalculate polynomial coefficients for x**n.
	   -- x**1 does not have a polynomial coefficient. */
	x2poly = -1.0 - nuf2;
	
	x3poly = -1.0 - 2 * tf2 - nuf2;
	
	x4poly = 5.0 + 3.0 * tf2 + 6.0 * nuf2 - 6.0 * tf2 * nuf2
		- 3.0 * (nuf2 *nuf2) - 9.0 * tf2 * (nuf2 * nuf2);
	
	x5poly = 5.0 + 28.0 * tf2 + 24.0 * tf4 + 6.0 * nuf2 + 8.0 * tf2 * nuf2;
	
	x6poly = -61.0 - 90.0 * tf2 - 45.0 * tf4 - 107.0 * nuf2
		+ 162.0 * tf2 * nuf2;
	
	x7poly = -61.0 - 662.0 * tf2 - 1320.0 * tf4 - 720.0 * (tf4 * tf2);
	
	x8poly = 1385.0 + 3633.0 * tf2 + 4095.0 * tf4 + 1575 * (tf4 * tf2);
		
	/* Calculate latitude */
	philambda[0] = phif + x2frac * x2poly * (x * x)
		+ x4frac * x4poly * Math.pow (x, 4.0)
		+ x6frac * x6poly * Math.pow (x, 6.0)
		+ x8frac * x8poly * Math.pow (x, 8.0);
		
	/* Calculate longitude */
	philambda[1] = lambda0 + x1frac * x
		+ x3frac * x3poly * Math.pow (x, 3.0)
		+ x5frac * x5poly * Math.pow (x, 5.0)
		+ x7frac * x7poly * Math.pow (x, 7.0);
		
	return;
}
 

function UTMXYToLatLon (x, y, zone, southhemi, latlon)
{
	var cmeridian;
		
	x -= 500000.0;
	x /= UTMScaleFactor;
		
	/* If in southern hemisphere, adjust y accordingly. */
	if (southhemi)
		y -= 10000000.0;
			
	y /= UTMScaleFactor;
	
	cmeridian = UTMCentralMeridian (zone);
	MapXYToLatLon (x, y, cmeridian, latlon);
		
	return;
}

//x,y en metros, x = Easting, y = Northing
//southhemi indica si se esta en hemisfero sur
//zone numero entero/real
//devuelve {lat, lon}
function UTMToLatLong(southhemi, zone, x, y)
{                                  
	var latlon = new Array(2);
	
	
	if ((zone < 1) || (60 < zone)) {
		alert ("The UTM zone you entered is out of range.  " +
			   "Please enter a number in the range [1, 60].");
		return false;
	}
				
	
	//validate x, UTM eastings range from 167,000 meters to 833,000 meters at the equator  
	if ((x < 167000) || (x > 8330000)) {
		alert ("Easting in the x field is out of range.");
		return false;
	}
	  	
	//validate y  Northing value must be in the range of 0 to 9,999,999.
	if ((y < 0)|| (y > 9999999)) {
		alert ("Northing in the y field is out of range.");
		return false;
	}
	
	UTMXYToLatLon (x, y, zone, southhemi, latlon);	
	
	//Check lat lon values are in range
	lon= RadToDeg (latlon[1])
	if (((lon<-180)) ||  (( lon>180))) {
	alert ("Calculated Longitude is out of range. Check Northing");
		return false;
	}
	
	lat= RadToDeg (latlon[0])
	if (((lat<-90)) ||  (( lat>90))) {
	alert ("Calculated Latitude out of range. Check Easting");
		return false;
	}
 

	return {lat:lat, lon:lon};
}
