import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;

import edu.cmu.meteor.scorer.MeteorConfiguration;
import edu.cmu.meteor.util.Constants;
import edu.cmu.meteor.util.Normalizer;
import edu.cmu.meteor.aligner.Aligner;
import edu.cmu.meteor.aligner.Alignment;
import edu.cmu.meteor.aligner.Match;
import edu.cmu.meteor.aligner.PartialAlignment;

public class ErrorCategorizer {
	
	Aligner aligner;
	Properties props;
	MeteorConfiguration config;
	private boolean normalize;
	private boolean keepPunctuation;
	private boolean lowerCase;
	
	private HashSet<String> functionWords;
	
	public ErrorCategorizer(Properties props)
	{
		this.props = props;
		this.config = new MeteorConfiguration(props);
		setNormalize(config.getNormalization());
		
		this.functionWords = new HashSet<String>();
		URL wordFileURL = config.getWordFileURL();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					wordFileURL.openStream(), "UTF-8"));
			String line;
			while ((line = in.readLine()) != null) {
				this.functionWords.add(line);
			}
			in.close();
		} catch (IOException ex) {
			throw new RuntimeException("No function word list ("
					+ wordFileURL.toString() + ")");
		}
		
	}

	public static void main(String[] args) {
		// Usage
				if (args.length < 3) {
					printUsage();
					System.exit(2);
				}

				// Files
				String testFile = args[0];
				String refFile = args[1];
				String srcfile = args[2];
				
				
				// Use command line options to initialize
				ErrorCategorizer categorizer = new ErrorCategorizer(createPropertiesFromArgs(args, 3));
				
				// Print settings
				
					System.out.println("Meteor version: " + Constants.VERSION);
					System.out.println();
					System.out.println("Eval ID:        " + categorizer.getConfigID());
					System.out.println();
					System.out.println("Language:       "
							+ categorizer.getLanguage().substring(0, 1).toUpperCase()
							+ categorizer.getLanguage().substring(1));
					System.out.println("Modules:        " + categorizer.getModulesString());
					System.out.println("Weights:        "
							+ categorizer.getModuleWeightsString());
					System.out.println("Parameters:     "
							+ categorizer.getParametersString());
					System.out.println();

				// Module / Weight check
				if (categorizer.getModuleWeights().size() < categorizer.getModules().size()) {
					System.err.println("Warning: More modules than weights specified "
							+ "- modules with no weights will not be counted.");
				}

				try {
					categorizer.prepAligner();
				} catch (IOException e) {
					System.err.println("Error: problem opening one of the input files:");
					e.printStackTrace();
				}


				try {
					categorizer.categorizeErrors(testFile, refFile, srcfile);
				} catch (IOException ex) {
						System.err.println("Error: Could not mark errors in test file '"+testFile+"' and ref file '"+refFile+"':");
						ex.printStackTrace();
						System.exit(1);
				}

	}
	
	private ArrayList<Integer> getModules() {
		return config.getModules();
	}

	private ArrayList<Double> getModuleWeights() {
		return config.getModuleWeights();
	}

	private String getParametersString() {
		return config.getParametersString();
	}

	private String getModuleWeightsString() {
		return config.getModuleWeightsString();
	}

	private String getModulesString() {
		return config.getModulesString();
	}

	private String getLanguage() {
		return config.getLanguage();
	}

	private String getConfigID() {
		return config.getConfigID();
	}

	private void prepAligner() throws IOException {
		
		// Language
				String language = props.getProperty("language");
				
				if (language == null)
					language = "english";
				language = Constants.normLanguageName(language);

				// Synonym Location
				String synDir = props.getProperty("synDir");
				URL synURL;
				if (synDir == null)
					synURL = Constants.DEFAULT_SYN_DIR_URL;
				else
					synURL = (new File(synDir)).toURI().toURL();

				// Paraphrase Location
				String paraFile = props.getProperty("paraFile");
				URL paraURL;
				if (paraFile == null)
					paraURL = Constants.getDefaultParaFileURL(Constants
							.getLanguageID(language));
				else
					paraURL = (new File(paraFile)).toURI().toURL();

				// Max Computations
				String beam = props.getProperty("beamSize");
				int beamSize = 0;
				if (beam == null)
					beamSize = Constants.DEFAULT_BEAM_SIZE;
				else
					beamSize = Integer.parseInt(beam);

				// Modules
				String modNames = props.getProperty("modules");
				if (modNames == null)
					modNames = "exact stem synonym paraphrase";
				ArrayList<Integer> modules = new ArrayList<Integer>();
				StringTokenizer mods = new StringTokenizer(modNames);
				while (mods.hasMoreTokens()) {
					int module = Constants.getModuleID(mods.nextToken());
					modules.add(module);
				}

				// Alignment Type
				String type = props.getProperty("type");
				if (type == null)
					type = "maxcov";
				Comparator<PartialAlignment> partialComparator;
				ArrayList<Double> moduleWeights = new ArrayList<Double>();
				if (type.equals("maxcov")) {
					partialComparator = Constants.PARTIAL_COMPARE_TOTAL;
					for (int module : modules) {
						if (module == Constants.MODULE_EXACT)
							moduleWeights.add(1.0);
						else if (module == Constants.MODULE_STEM)
							moduleWeights.add(0.5);
						else if (module == Constants.MODULE_SYNONYM)
							moduleWeights.add(0.5);
						else
							moduleWeights.add(0.5);
					}
				} // maxacc
				else {
					partialComparator = Constants.PARTIAL_COMPARE_TOTAL_ALL;
					for (int module : modules) {
						if (module == Constants.MODULE_EXACT)
							moduleWeights.add(1.0);
						else if (module == Constants.MODULE_STEM)
							moduleWeights.add(1.0);
						else if (module == Constants.MODULE_SYNONYM)
							moduleWeights.add(1.0);
						else
							moduleWeights.add(0.0);
					}
				}

				// Construct aligner
				aligner = new Aligner(language, modules, moduleWeights,
						beamSize, Constants.getDefaultWordFileURL(Constants
								.getLanguageID(language)), synURL, paraURL,
						partialComparator);
	}

	/**
	 * Categorize Errors
	 */

	public void categorizeErrors(String hypFile, String refFile, String srcFile)
			throws IOException {

		ArrayList<String> hypLines = new ArrayList<String>();
		ArrayList<String> refLines = new ArrayList<String>();
		ArrayList<String> srcLines = new ArrayList<String>();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(hypFile), "UTF-8"));
		String line;
		while ((line = in.readLine()) != null)
			hypLines.add(line);
		in.close();

		in = new BufferedReader(new InputStreamReader(new FileInputStream(
				refFile), "UTF-8"));
		while ((line = in.readLine()) != null)
				refLines.add(line);
		in.close();
		
		in = new BufferedReader(new InputStreamReader(new FileInputStream(
				srcFile), "UTF-8"));
		while ((line = in.readLine()) != null)
				srcLines.add(line);
		in.close();
		
		if (hypLines.size() != refLines.size() && hypLines.size() != srcLines.size()) {
			System.err.println("Error: test, reference, and source not same length");
			return;
		}

		for (int i = 0; i < hypLines.size(); i++) {
			String hypline = hypLines.get(i);
			String refline = refLines.get(i);
			String srcline = srcLines.get(i);
			
			// Normalize hyp & ref
			if (normalize) {
				hypline = Normalizer.normalizeLine(hypline, config.getLangID(), keepPunctuation);
				refline = Normalizer.normalizeLine(refline, config.getLangID(),	keepPunctuation);
			}
			// Lowercase all
			if (lowerCase) {
				hypline = hypline.toLowerCase();
				refline = refline.toLowerCase();
				srcline = srcline.toLowerCase();
			}
			
			// Get Alignment
			Alignment alignment = aligner.align(hypline, refline);
			
			// Print Alignment
			//alignment.printMatchedPhrases();
			printTaggedErrors(alignment,srcline);
		}

	}
	
	public static Properties createPropertiesFromArgs(String[] args,
			int startIndex) {
		Properties props = new Properties();
		int curArg = startIndex;
		while (curArg < args.length) {
			if (args[curArg].equals("-l")) {
				props.setProperty("language", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-t")) {
				props.setProperty("task", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-p")) {
				props.setProperty("parameters", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-m")) {
				props.setProperty("modules", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-w")) {
				props.setProperty("moduleWeights", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-r")) {
				props.setProperty("refCount", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-x")) {
				props.setProperty("beamSize", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-s")) {
				props.setProperty("wordFile", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-d")) {
				props.setProperty("synDir", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-a")) {
				props.setProperty("paraFile", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-f")) {
				props.setProperty("filePrefix", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-new")) {
				props.setProperty("newLang", "true");
				props.setProperty("filesDir", args[curArg + 1]);
				curArg += 2;
			} else if (args[curArg].equals("-ch")) {
				props.setProperty("charBased", "true");
				curArg += 1;
			} else if (args[curArg].equals("-q")) {
				props.setProperty("quiet", "true");
				curArg += 1;
			} else if (args[curArg].equals("-writeAlignments")) {
				props.setProperty("writeAlignments", "true");
				curArg += 1;
			} else if (args[curArg].equals("-norm")) {
				props.setProperty("norm", "true");
				curArg += 1;
			} else if (args[curArg].equals("-lower")) {
				props.setProperty("lower", "true");
				curArg += 1;
			} else if (args[curArg].equals("-sgml")) {
				props.setProperty("sgml", "true");
				curArg += 1;
				// Include -mira for backward compatibility
			} else if (args[curArg].equals("-stdio")
					|| args[curArg].equals("-mira")) {
				props.setProperty("stdio", "true");
				curArg += 1;
			} else if (args[curArg].equals("-noPunct")) {
				props.setProperty("noPunct", "true");
				curArg += 1;
			} else if (args[curArg].equals("-ssOut")) {
				props.setProperty("ssOut", "true");
				curArg += 1;
			} else if (args[curArg].equals("-vOut")) {
				props.setProperty("vOut", "true");
				curArg += 1;
			} else {
				System.err.println("Unknown option \"" + args[curArg] + "\"");
				System.exit(1);
			}
			String params = props.getProperty("parameters");
			if (params != null)
				props.setProperty("task", "custom (" + params + ")");
		}
		return props;
	}

	private static void printUsage() {
		System.err
				.println("Usage: java -Xmx2G -cp meteor-*.jar ErrorCategorizer <test> <reference> <source> [options]");
		System.err.println();
		System.err.println("Options:");
		System.err
				.println("-l language                     Fully supported: en cz de es fr");
		System.err
				.println("                                Supported with language-independent parameters:");
		System.err
				.println("                                  da fi hu it nl no pt ro ru se tr");
		System.err.println("                                Experimental:");
		System.err.println("                                  ar-bw-red");

		System.err
				.println("-p 'alpha beta gamma delta'     Custom parameters (overrides default)");
		System.err
				.println("-m 'module1 module2 ...'        Specify modules (overrides default)");
		System.err
				.println("                                  Any of: exact stem synonym paraphrase");
		System.err
				.println("-w 'weight1 weight2 ...'        Specify module weights (overrides default)");
		System.err.println("-x beamSize                     (default 40)");
		System.err
				.println("-s wordListFile                 (if not default for language)");
		System.err
				.println("-d synonymDirectory             (if not default for language)");
		System.err
				.println("-a paraphraseFile               (if not default for language)");
		System.err
				.println("-f filePrefix                   Prefix for output files (default 'meteor')");
		System.err
				.println("-new files-dir                  New language! (files-dir contains function.words and paraphrase.gz)");
		System.err.println("                                  implies -lower");
		System.err
				.println("-norm                           Tokenize / normalize punctuation and lowercase");
		System.err
				.println("                                  (Recommended unless scoring raw output with");
		System.err
				.println("                                   pretokenized references)");
		System.err
				.println("-lower                          Lowercase only (not required if -norm specified)");
		System.err
				.println("-noPunct                        Do not consider punctuation when scoring");
		System.err
				.println("                                  (Not recommended unless special case)");
		System.err.println();
		System.err.println("Sample options for plaintext: -l <lang> -norm");
		System.err
				.println("Sample options for raw output / pretokenized references: -l <lang> -lower");
		System.err
				.println("Sample options for new language (plaintext): -new meteor-files");
		System.err.println();

		System.err.println("See README file for additional information");
	}
	private void setNormalize(int normtype) {
		if (normtype == Constants.NORMALIZE_LC_ONLY) {
			normalize = false;
			keepPunctuation = true;
			lowerCase = true;
		} else if (normtype == Constants.NORMALIZE_KEEP_PUNCT) {
			normalize = true;
			keepPunctuation = true;
			lowerCase = true;

		} else if (normtype == Constants.NORMALIZE_NO_PUNCT) {
			normalize = true;
			keepPunctuation = false;
			lowerCase = true;
		} else {
			// Assume NO_NORMALIZE
			normalize = false;
			keepPunctuation = true;
			lowerCase = false;
		}
	}

	// Print out the reference and hypothesis with erros marged
		public void printTaggedErrors(Alignment alignment,String source) {
			Match[] matches = alignment.matches;
			//System.out.println(alignment.words1);
			//System.out.println(alignment.words2);
						
			int hyplen = alignment.words1.size();
			boolean[] matchedhyp = new boolean[hyplen];
			Arrays.fill(matchedhyp, false);
			
			ArrayList<String> labeledhwords = (ArrayList<String>) alignment.words1.clone();
			ArrayList<String> labeledrmatches = new ArrayList<String>(Collections.nCopies(labeledhwords.size(),null));
			ArrayList<String> labeledhmatches = new ArrayList<String>(Collections.nCopies(labeledhwords.size(),null));

			// TODO: Add alignment line
			
			System.out.print("...ref-err-cats: ");
			for (int i=0; i<matches.length; i++) {
				Match m = matches[i];
				if (m != null) {
					String label = "~~x";
					switch(m.module)
					{
					case 1:
						label = "~~infl";
						break;
					case 2:
						label = "~~syn";
						break;
					case 3:
						label = "~~para";
						break;
					default: break;
						
					}
					StringBuilder refmatch = new StringBuilder();
					for (int j = m.start; j < m.start + m.length; j++)
					{
						String labelmod = "";
						String rword = alignment.words2.get(j);
						if(functionWords.contains(rword))
							labelmod = "_f";
						System.out.print(rword+label+labelmod+ " ");
						refmatch.append(rword);
						refmatch.append(" ");
					}
					//refmatch.append(label);

					labeledrmatches.set(m.matchStart, refmatch.toString().trim());
					StringBuilder hypmatch = new StringBuilder();
					for (int j=m.matchStart; j<m.matchStart+m.matchLength;j++)
					{
						String hword = alignment.words1.get(j);
						String labelmod = "";
						if(functionWords.contains(hword))
							labelmod = "_f";
						labeledhwords.set(j, hword+label+labelmod);
						matchedhyp[j] = true;
						hypmatch.append(hword);
						hypmatch.append(" ");
					}
					labeledhmatches.set(m.matchStart, hypmatch.toString().trim()+label);
					i += m.length-1;
				}
				else {
					String rword = alignment.words2.get(i);
					System.out.print(rword+"~~miss");
					if(functionWords.contains(rword))
						System.out.print("_f");
					System.out.print(" ");
				}
			}
			StringBuilder alignString = new StringBuilder("--algn-err-cats: ");
			System.out.println();
			System.out.print("...hyp-err-cats: ");
			int matchend = 0;
			String currmatch = null;
			for(int i=0; i<hyplen; i++)
			{
				String currword = labeledhwords.get(i);
				System.out.print(currword);
				String word = currword.split("~~")[0];
				
				boolean isFunc = functionWords.contains(word);
				String label = "";
				if(!matchedhyp[i])
				{
					label = "~~extr";
					if(isFunc)
					{
						label = "~~extr_f";
					}
					else
					{
						if(source.toLowerCase().contains(word))
							label = "~~OOV";
					}
				}
				System.out.print(label+" ");
				if(i==matchend)
				{
					currmatch = labeledhmatches.get(i);
					if(currmatch != null)
					{
						alignString.append(currmatch.replace(' ', '_'));
						matchend = i+currmatch.split(" ").length;
					}
					else
					{
						alignString.append(currword);
						matchend = i+1;
					}
					
					alignString.append(label);
					String rmatch = labeledrmatches.get(i);
					alignString.append("||");
					if(rmatch != null)
						alignString.append(rmatch.replace(' ', '_'));
					else
						alignString.append("<>");
					alignString.append(" ");
				}
			}
			System.out.println();
			System.out.println(alignString);
			System.out.println();
		}
}

