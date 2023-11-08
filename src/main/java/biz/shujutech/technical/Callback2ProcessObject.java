/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.shujutech.technical;

/**
 *
 */
@FunctionalInterface
public interface Callback2ProcessObject {
	public boolean processObject(Object aObject) throws Exception;
	
}
