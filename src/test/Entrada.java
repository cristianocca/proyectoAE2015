/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

/**
 *
 * @author Cristiano
 */
 public class Entrada{
    public int N;       //cantidad de antenas
    public int[] C;     //Costo de antenas
    public int[] CB;    //Cobertura de antenas
    public int CBMin;   //Cobertura minima


    public Entrada(int N, int[] C, int[] CB, int CBMin){
        this.N = N;
        this.C = C;
        this.CB = CB;
        this.CBMin = CBMin;
    }
}
