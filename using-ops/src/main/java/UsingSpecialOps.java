/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imagej.ops.special.chain.IIs;
import net.imagej.ops.special.chain.RTs;
import net.imagej.ops.special.computer.Computers;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imagej.ops.special.hybrid.UnaryHybridCF;
import net.imagej.ops.special.inplace.UnaryInplaceOp;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.DoubleType;

import org.scijava.log.LogService;

/**
 * This tutorial shows how to use the "special" ImageJ Ops: computer, function,
 * hybrid and inplace.
 * 
 * @see net.imagej.ops.special.SpecialOp
 */
public class UsingSpecialOps {
	@SuppressWarnings("unused")
	public static void main(final String... args) throws Exception {
		ImageJ ij = new ImageJ();
		LogService log = ij.log();
		OpService ops = ij.op();

		// create a blank iterableInterval
		IterableInterval<DoubleType> image = ArrayImgs.doubles(256, 256);
		IterableInterval<DoubleType> output = ArrayImgs.doubles(256, 256);
		DoubleType in = new DoubleType(10.0);
		DoubleType out = new DoubleType();

		log.info(
			"--------- Computer op: Stores results in an output reference ---------");
		UnaryComputerOp<DoubleType, DoubleType> add5 = //
			Computers.unary(ops, Ops.Math.Add.class, DoubleType.class,
				DoubleType.class, 5.0);
		add5.compute1(in, out); // Add 5 to 'in' and stores result in 'out'
		log.info("Out = " + out + "\n");

		log.info(
			"--------- Call helper class to get around raw types if using generics as I/O  ---------");
		log.info(
			"--------- Without calling helper class, you get unsafe raw types ---------");
		@SuppressWarnings("rawtypes")
		UnaryComputerOp<IterableInterval, RandomAccessibleInterval> badAdd =
			Computers.unary(ops, Ops.Math.Add.class, RandomAccessibleInterval.class,
				IterableInterval.class, in);
		RandomAccessibleInterval<ByteType> byteImage = ArrayImgs.bytes(256, 256);
		// Uncomment and run: Will cause a runtime error, bad!
		// badAdd.compute1(image, byteImage);
		log.info(
			"--------- There are currently three helper classes which help with type safety ---------");
		log.info(
			"--------- RAIs, IIs, and RTs for when RandomAccessibleInterval, IterableInterval, and RealType is the output, respectively ---------\n");
		UnaryComputerOp<IterableInterval<DoubleType>, IterableInterval<DoubleType>> goodAdd =
			Computers.unary(ops, Ops.Math.Add.class, image, image, in);
		// Uncomment: Will get a compiler error instead, better
		// goodAdd.compute1(image, byteImage);

		UnaryComputerOp<String, IterableInterval<DoubleType>> equation = //
			Computers.unary(ops, Ops.Image.Equation.class, image, "p[0]+p[1]");
		equation.compute1("p[0]+p[1]", image); // Apply the equation to the 'image'
		ij.ui().show("Image", image); // Show the 'image' in a window

		log.info(
			"--------- Function op: Returns the result as a new object ---------");
		UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType> functionMean = //
			RTs.function(ops, Ops.Stats.Mean.class, image);
		// Compute the mean value of the iterable and return it as a new object
		DoubleType m = functionMean.compute1(image);
		log.info("--------- Stats: Mean = " + m + " ---------\n");

		log.info("--------- Inplace op: mutate the given input ---------");
		log.info(
			"--------- Loop op: Execute op on the input for a certain number of times --------");
		int iterations = 4;
		UnaryInplaceOp<DoubleType> addLoop = RTs.inplace(ops, Ops.Loop.class, in, add5,
			iterations);
		// Add 5 to 'in' each time through the loop; store value in 'in'.
		addLoop.mutate(in);
		log.info("--------- 'In' is modified from 10.0 to " + in + " ---------\n");

		log.info(
			"--------- Hybrid op: Can be used either as a function or computer op ---------");
		UnaryHybridCF<IterableInterval<DoubleType>, DoubleType> meanOp = //
			RTs.hybrid(ops, Ops.Stats.Mean.class, image);
		UnaryHybridCF<IterableInterval<DoubleType>, DoubleType> maxOp = //
			RTs.hybrid(ops, Ops.Stats.Max.class, image);
		DoubleType mean = new DoubleType(0);
		// Use the HybridOp as a ComputerOp: store result in output reference.
		meanOp.compute1(image, mean);
		// Use the HybridOp as a FunctionOp: return result as a new object.
		DoubleType max = maxOp.compute1(image);
		log.info("--------- Stats: Mean = " + mean + " Max = " + max +
			" ---------\n");

		log.info("-------- Map op: Execute op on every pixel of an image --------");
		UnaryComputerOp<IterableInterval<DoubleType>, IterableInterval<DoubleType>> mapOp =
			IIs.computer(ops, Ops.Map.class, image, add5);
		// Add 5 to each pixel, and return the output as the given output reference.
		mapOp.compute1(image, output);
		log.info("-------- Input mean: " + meanOp.compute1(image) +
			" Output mean: " + meanOp.compute1(output) + " --------\n");
		ij.ui().show("Map output", output);

		log.info("--------- Searching for the op is slower: ---------");
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			// Search for the op every time and then run it
			ops.math().add(out, in, 5);
		}
		long mid = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			// Just run the op
			add5.compute1(in, out);
		}
		long end = System.currentTimeMillis();
		log.info("--------- Slow = " + (mid - start) + "ms ---------");
		log.info("--------- Fast = " + (end - mid) + "ms ---------\n");

		log.info("--------- All done! ---------");
	}
}
