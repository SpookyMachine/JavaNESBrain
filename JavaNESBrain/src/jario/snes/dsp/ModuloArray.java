/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.dsp;

public class ModuloArray
{
	public int read(int index)
	{
		return buffer[size + index];
	}

	public void write(int index, int value)
	{
		buffer[index] = buffer[index + size] = buffer[index + size + size] = value;
	}

	public ModuloArray(int size_)
	{
		size = size_;
		buffer = new int[size * 3];
	}

	private int size;
	private int[] buffer;
}
