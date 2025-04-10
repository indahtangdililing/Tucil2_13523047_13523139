package image;

import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import quadtree.QuadtreeNode;
import java.awt.image.*;
import java.io.*;
import java.util.Iterator;


public class GifSequenceWriter {
    protected ImageWriter gifWriter;
    protected ImageWriteParam imageWriteParam;
    protected IIOMetadata imageMetaData;
    

    public GifSequenceWriter(
        ImageOutputStream outputStream,int imageType, int timeBetweenFramesMS,
        boolean loopContinuously) throws IIOException, IOException {
      gifWriter = getWriter(); 
      imageWriteParam = gifWriter.getDefaultWriteParam();
      ImageTypeSpecifier imageTypeSpecifier =
        ImageTypeSpecifier.createFromBufferedImageType(imageType);
  
      imageMetaData =
        gifWriter.getDefaultImageMetadata(imageTypeSpecifier,
        imageWriteParam);
  
      String metaFormatName = imageMetaData.getNativeMetadataFormatName();
  
      IIOMetadataNode root = (IIOMetadataNode)
        imageMetaData.getAsTree(metaFormatName);
  
      IIOMetadataNode graphicsControlExtensionNode = getNode(
        root,
        "GraphicControlExtension");
  
      graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
      graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
      graphicsControlExtensionNode.setAttribute(
        "transparentColorFlag",
        "FALSE");
      graphicsControlExtensionNode.setAttribute(
        "delayTime",
        Integer.toString(timeBetweenFramesMS / 10));
      graphicsControlExtensionNode.setAttribute(
        "transparentColorIndex",
        "0");
  
      IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
      commentsNode.setAttribute("CommentExtension", "Created by MAH");
  
      IIOMetadataNode appEntensionsNode = getNode(
        root,
        "ApplicationExtensions");
  
      IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
  
      child.setAttribute("applicationID", "NETSCAPE");
      child.setAttribute("authenticationCode", "2.0");
  
      int loop = loopContinuously ? 0 : 1;
  
      child.setUserObject(new byte[]{ 0x1, (byte) (loop & 0xFF), (byte)
        ((loop >> 8) & 0xFF)});
      appEntensionsNode.appendChild(child);
  
      imageMetaData.setFromTree(metaFormatName, root);
  
      gifWriter.setOutput(outputStream);
  
      gifWriter.prepareWriteSequence(null);
    }
    
    public void writeToSequence(RenderedImage img) throws IOException {
      gifWriter.writeToSequence(
        new IIOImage(
          img,
          null,
          imageMetaData),
        imageWriteParam);
    }

    public static void reconstructImageByDepth(QuadtreeNode node, int[][][] image, int originalWidth, int originalHeight, int currentDepth) {
      if (node == null) {
          return;
      }
  
      // Jika node adalah daun (tidak memiliki anak), isi blok dengan warna rata-rata
      if (node.children[0] == null) {
          for (int i = 0; i < node.height; i++) {
              for (int j = 0; j < node.width; j++) {
                  if (node.y + i < originalHeight && node.x + j < originalWidth) {
                      image[node.y + i][node.x + j] = node.avgColor;
                  }
              }
          }
      } else if (currentDepth == 1) {
          // Jika kedalaman saat ini adalah 1, isi blok dengan warna rata-rata
          for (int i = 0; i < node.height; i++) {
              for (int j = 0; j < node.width; j++) {
                  if (node.y + i < originalHeight && node.x + j < originalWidth) {
                      image[node.y + i][node.x + j] = node.avgColor;
                  }
              }
          }
      } else {
          // Rekursi ke anak-anak jika kedalaman lebih besar dari 1
          boolean hasChildren = false;
          for (QuadtreeNode child : node.children) {
              if (child != null) {
                  hasChildren = true;
                  reconstructImageByDepth(child, image, originalWidth, originalHeight, currentDepth - 1);
              }
          }
  
          // Jika tidak ada anak yang valid, gunakan blok terakhir yang paling dalam
          if (!hasChildren) {
              for (int i = 0; i < node.height; i++) {
                  for (int j = 0; j < node.width; j++) {
                      if (node.y + i < originalHeight && node.x + j < originalWidth) {
                          image[node.y + i][node.x + j] = node.avgColor;
                      }
                  }
              }
          }
      }
    }

    public void close() throws IOException {
      gifWriter.endWriteSequence();    
    }

    private static ImageWriter getWriter() throws IIOException {
      Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
      if(!iter.hasNext()) {
        throw new IIOException("No GIF Image Writers Exist");
      } else {
        return iter.next();
      }
    }

    private static IIOMetadataNode getNode(
        IIOMetadataNode rootNode,
        String nodeName) {
      int nNodes = rootNode.getLength();
      for (int i = 0; i < nNodes; i++) {
        if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName)
            == 0) {
          return((IIOMetadataNode) rootNode.item(i));
        }
      }
      IIOMetadataNode node = new IIOMetadataNode(nodeName);
      rootNode.appendChild(node);
      return(node);
    }
}
  //   public static void main(String[] args) throws Exception {
  //     if (args.length > 1) {
  //       // grab the output image type from the first image in the sequence
  //       BufferedImage firstImage = ImageIO.read(new File(args[0]));
  
  //       // create a new BufferedOutputStream with the last argument
  //       ImageOutputStream output = 
  //         new FileImageOutputStream(new File(args[args.length - 1]));
        
  //       // create a gif sequence with the type of the first image, 1 second
  //       // between frames, which loops continuously
  //       GifSequenceWriter writer = 
  //         new GifSequenceWriter(output, firstImage.getType(), 1, false);
        
  //       // write out the first image to our sequence...
  //       writer.writeToSequence(firstImage);
  //       for(int i=1; i<args.length-1; i++) {
  //         BufferedImage nextImage = ImageIO.read(new File(args[i]));
  //         writer.writeToSequence(nextImage);
  //       }
        
  //       writer.close();
  //       output.close();
  //     } else {
  //       System.out.println(
  //         "Usage: java GifSequenceWriter [list of gif files] [output file]");
  //     }
  //   }
  // }

// public class GIFGenerator {

//     public static void generateGIF(List<BufferedImage> images, String outputFilePath, int delay) throws IOException {
//         if (images == null || images.isEmpty()) {
//             throw new IllegalArgumentException("Image list cannot be null or empty.");
//         }

//         // Create an output stream for the GIF file
//         try (ImageOutputStream output = new FileImageOutputStream(new File(outputFilePath))) {
//             AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
//             gifEncoder.start(output); // Start encoding
//             gifEncoder.setDelay(delay); // Set delay between frames
//             gifEncoder.setRepeat(0); // Set loop count (0 = infinite loop)

//             // Add each image to the GIF
//             for (BufferedImage image : images) {
//                 gifEncoder.addFrame(image);
//             }

//             gifEncoder.finish(); // Finalize the GIF
//         }
//     }

//     public static void saveGIF(List<BufferedImage> images, String outputPath, int delay) throws IOException {
//         File outputFile = new File(outputPath);

//         // Ensure the parent directories exist
//         if (!outputFile.getParentFile().exists()) {
//             if (!outputFile.getParentFile().mkdirs()) {
//                 throw new IOException("Failed to create directories for output path: " + outputPath);
//             }
//         }

//         // Generate the GIF
//         generateGIF(images, outputFile.getAbsolutePath(), delay);
//     }
// }

