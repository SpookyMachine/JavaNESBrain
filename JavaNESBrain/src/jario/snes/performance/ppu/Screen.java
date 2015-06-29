/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.performance.ppu;

import java.nio.ShortBuffer;
import java.util.Arrays;

public class Screen
{
	class Regs
	{
		public boolean addsub_mode;
		public boolean direct_color;
		public boolean color_mode;
		public boolean color_halve;
		public boolean[] color_enable = new boolean[7];
		public int color_b;
		public int color_g;
		public int color_r;
		public int color;
	}

	class Output
	{
		public class Pixel
		{
			public int color;
			public int priority;
			public int source;
		}

		public Pixel[] main = new Pixel[256];
		public Pixel[] sub = new Pixel[256];

		public Output()
		{
			for (int i = 0; i < main.length; i++)
				main[i] = new Pixel();
			for (int i = 0; i < sub.length; i++)
				sub[i] = new Pixel();
		}

		public void plot_main(int x, int color, int priority, int source)
		{
			if (priority > main[x].priority)
			{
				main[x].color = color;
				main[x].priority = priority;
				main[x].source = source;
			}
		}

		public void plot_sub(int x, int color, int priority, int source)
		{
			if (priority > sub[x].priority)
			{
				sub[x].color = color;
				sub[x].priority = priority;
				sub[x].source = source;
			}
		}
	}

	public Regs regs = new Regs();
	public Output output = new Output();

	public ColorWindow window = new ColorWindow();
	public short[][] light_table;

	public int get_palette(int color)
	{
		return (self.cgram[(color * 2) + 0] + (self.cgram[(color * 2) + 1] << 8)) & 0xFFFF;
	}

	public int get_direct_color(int palette, int tile)
	{
		return ((tile & 7) << 2) | ((palette & 1) << 1) |
				(((tile >> 3) & 7) << 7) | (((palette >> 1) & 1) << 6) |
				((tile >> 6) << 13) | ((palette >> 2) << 12);
	}

	public int addsub(int x, int y, boolean halve)
	{
		if (!regs.color_mode)
		{
			if (!halve)
			{
				int sum = x + y;
				int carry = (sum - ((x ^ y) & 0x0421)) & 0x8420;
				return ((sum - carry) | (carry - (carry >> 5))) & 0xFFFF;
			}
			else
			{
				return ((x + y - ((x ^ y) & 0x0421)) >> 1) & 0xFFFF;
			}
		}
		else
		{
			int diff = x - y + 0x8420;
			int borrow = (diff - ((x ^ y) & 0x8420)) & 0x8420;
			if (!halve)
			{
				return ((diff - borrow) & (borrow - (borrow >> 5))) & 0xFFFF;
			}
			else
			{
				return ((((diff - borrow) & (borrow - (borrow >> 5))) & 0x7bde) >> 1) & 0xFFFF;
			}
		}
	}

	public void scanline()
	{
		int main_color = get_palette(0);
		int sub_color = (self.regs.pseudo_hires == false && self.regs.bgmode != 5 && self.regs.bgmode != 6) ? regs.color : main_color;

		for (int x = 0; x < 256; x++)
		{
			output.main[x].color = main_color;
			output.main[x].priority = 0;
			output.main[x].source = 6;

			output.sub[x].color = sub_color;
			output.sub[x].priority = 0;
			output.sub[x].source = 6;
		}

		window.render(false);
		window.render(true);
	}

	public void render_black()
	{
		byte[] data = self.output.array();
		int data_offset = (self.ppuCounter.vcounter() * 1024) << 1;

		if (self.interlace() && self.ppuCounter.field())
		{
			data_offset += (512 << 1);
		}

		Arrays.fill(data, data_offset, data_offset + (self.display.width << 2), (byte) 0);
	}

	public int get_pixel_main(int x)
	{
		Output.Pixel main = output.main[x];
		Output.Pixel sub = output.sub[x];

		if (!regs.addsub_mode)
		{
			sub.source = 6;
			sub.color = regs.color;
		}

		if ((window.main[x] == 0))
		{
			if ((window.sub[x] == 0)) { return 0x0000; }
			main.color = 0x0000;
		}

		if (main.source != 5 && regs.color_enable[main.source] && (window.sub[x] != 0))
		{
			boolean halve = false;
			if (regs.color_halve && (window.main[x] != 0))
			{
				if (!regs.addsub_mode || sub.source != 6)
				{
					halve = true;
				}
			}
			return addsub(main.color, sub.color, halve);
		}

		return main.color & 0xFFFF;
	}

	public int get_pixel_sub(int x)
	{
		Output.Pixel main = output.sub[x];
		Output.Pixel sub = output.main[x];

		if (!regs.addsub_mode)
		{
			sub.source = 6;
			sub.color = regs.color;
		}

		if ((window.main[x] == 0))
		{
			if ((window.sub[x] == 0)) { return 0x0000; }
			main.color = 0x0000;
		}

		if (main.source != 5 && regs.color_enable[main.source] && (window.sub[x] != 0))
		{
			boolean halve = false;
			if (regs.color_halve && (window.main[x] != 0))
			{
				if (!regs.addsub_mode || sub.source != 6)
				{
					halve = true;
				}
			}
			return addsub(main.color, sub.color, halve);
		}

		return main.color & 0xFFFF;
	}

	public void render()
	{
		ShortBuffer data = self.output.asShortBuffer();
		int data_offset = self.ppuCounter.vcounter() * 1024;

		if (self.interlace() && self.ppuCounter.field())
		{
			data_offset += 512;
		}
		short[] light = light_table[self.regs.display_brightness];

		if (!self.regs.pseudo_hires && self.regs.bgmode != 5 && self.regs.bgmode != 6)
		{
			for (int i = 0; i < 256; i++)
			{
				data.put(data_offset + i, light[get_pixel_main(i)]);
			}
		}
		else
		{
			int arrayIndex = 0;
			for (int i = 0; i < 256; i++)
			{
				data.put(data_offset + arrayIndex++, light[get_pixel_sub(i)]);
				data.put(data_offset + arrayIndex++, light[get_pixel_main(i)]);
			}
		}
	}

	public Screen(PPU self)
	{
		this.self = self;
		light_table = new short[16][];
		for (int l = 0; l < 16; l++)
		{
			light_table[l] = new short[32768];
			for (int r = 0; r < 32; r++)
			{
				for (int g = 0; g < 32; g++)
				{
					for (int b = 0; b < 32; b++)
					{
						double luma = (double) l / 15.0;
						int ar = (int) (luma * r + 0.5);
						int ag = (int) (luma * g + 0.5);
						int ab = (int) (luma * b + 0.5);
						light_table[l][(r << 10) + (g << 5) + (b << 0)] = (short) ((ab << 10) + (ag << 5) + (ar << 0));
					}
				}
			}
		}
	}

	private PPU self;
}
