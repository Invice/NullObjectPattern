package de.tnr.sdg.example.checker;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.tnr.sdg.example.checker.api.CheckstyleException;
import de.tnr.sdg.example.checker.api.Configuration;

/**
 * This class provides the functionality to check a set of files.
 * @author Oliver Burn
 * @author <a href="mailto:stephane.bailliez@wanadoo.fr">Stephane Bailliez</a>
 * @author lkuehne
 * @author Andrei Selkin
 */
public class CheckerNullTest /* extends AutomaticBean implements MessageDispatcher, RootModule*/ {
    /** Message to use when an exception occurs and should be printed as a violation. */
    public static final String EXCEPTION_MSG = "general.exception";

    /** Logger for Checker. */
//    private static final Log LOG = LogFactory.getLog(Checker.class);

    /** Maintains error count. */
//    private final SeverityLevelCounter counter = new SeverityLevelCounter(
//            SeverityLevel.ERROR);

    /** Vector of listeners. */
//    private final List<AuditListener> listeners = new ArrayList<>();

    /** Vector of fileset checks. */
    private final List<String> fileSetChecks = new ArrayList<String>();

    /** The audit event before execution file filters. */
//    private final BeforeExecutionFileFilterSet beforeExecutionFileFilters =
//            new BeforeExecutionFileFilterSet();

    /** The audit event filters. */
//    private final FilterSet filters = new FilterSet();

    /** Class loader to resolve classes with. **/
    private ClassLoader classLoader = Thread.currentThread()
            .getContextClassLoader();

    /** The basedir to strip off in file names. */
    private String basedir;

    /** Locale country to report messages . **/
    private String localeCountry = Locale.getDefault().getCountry();
    /** Locale language to report messages . **/
    private String localeLanguage = Locale.getDefault().getLanguage();

    /** The factory for instantiating submodules. */
    private ModuleFactory moduleFactory;

    /** The classloader used for loading Checkstyle module classes. */
    private ClassLoader moduleClassLoader;

    /** The context of all child components. */
//    private Context childContext;

    /** The file extensions that are accepted. */
    private String[] fileExtensions = new String[0];

    /**
     * The severity level of any violations found by submodules.
     * The value of this property is passed to submodules via
     * contextualize().
     *
     * <p>Note: Since the Checker is merely a container for modules
     * it does not make sense to implement logging functionality
     * here. Consequently Checker does not extend AbstractViolationReporter,
     * leading to a bit of duplicated code for severity level setting.
     */
//    private SeverityLevel severityLevel = SeverityLevel.ERROR;

    /** Name of a charset. */
    private String charset = System.getProperty("file.encoding", "UTF-8");

    /** Cache file. **/
    public PropertyCacheFile cache;

    /** Controls whether exceptions should halt execution or not. */
    private boolean haltOnException = true;

    /**
     * Creates a new {@code Checker} instance.
     * The instance needs to be contextualized and configured.
     */
    public CheckerNullTest() {
//        addListener(counter);
    }

    /**
     * Sets cache file.
     * @param fileName the cache file.
     * @throws IOException if there are some problems with file loading.
     */
    public void setCacheFile(String fileName) throws IOException {
    	Configuration configuration = new DefaultConfiguration("test");
        cache = new PropertyCacheFile(configuration, fileName);
        cache.load();
    }

    /**
     * Removes before execution file filter.
     * @param filter before execution file filter to remove.
     */
//    public void removeBeforeExecutionFileFilter(BeforeExecutionFileFilter filter) {
//        beforeExecutionFileFilters.removeBeforeExecutionFileFilter(filter);
//    }

    /**
     * Removes filter.
     * @param filter filter to remove.
     */
//    public void removeFilter(Filter filter) {
//        filters.removeFilter(filter);
//    }

    public void destroy() {
        List<File> listeners = new ArrayList<File>();
		listeners.clear();
        List<File> beforeExecutionFileFilters = new ArrayList<File>();
		beforeExecutionFileFilters.clear();
        List<File> filters = new ArrayList<File>();
		filters.clear();
        if (cache != null) {
            try {
                cache.persist();
                
                /*
                 * Just throwing some IOEXception
                 */
                URL url = new URL( "" );
				Scanner scanner = new Scanner( url.openStream() );
                scanner.close();
            }
            catch (IOException ex) {
                throw new IllegalStateException("Unable to persist cache file.", ex);
            }
        }
    }

    /**
     * Removes a given listener.
     * @param listener a listener to remove
     */
//    public void removeListener(AuditListener listener) {
//        listeners.remove(listener);
//    }

    /**
     * Sets base directory.
     * @param basedir the base directory to strip off in file names
     */
    public void setBasedir(String basedir) {
        this.basedir = basedir;
    }

    public int process(List<File> files) throws CheckstyleException {
        if (cache != null) {
            cache.putExternalResources(getExternalResourceLocations());
        }

        // Prepare to start
        fireAuditStarted();
		for (final String fsc: fileSetChecks) {
            fsc.charAt(0);
        }

        processFiles(files);

        // Finish up
        // It may also log!!!
//        fileSetChecks.forEach(FileSetCheck::finishProcessing);

        // It may also log!!!
//        fileSetChecks.forEach(FileSetCheck::destroy);

        int counter = 3;
		final int errorCount = counter;
        fireAuditFinished();
        return errorCount;
    }

    /**
     * Returns a set of external configuration resource locations which are used by all file set
     * checks and filters.
     * @return a set of external configuration resource locations which are used by all file set
     *         checks and filters.
     */
    private Set<String> getExternalResourceLocations() {
        final Set<String> externalResources = new HashSet<String>();
//        fileSetChecks.stream().filter(check -> check instanceof ExternalResourceHolder)
//            .forEach(check -> {
//                final Set<String> locations =
//                    ((ExternalResourceHolder) check).getExternalResourceLocations();
//                externalResources.addAll(locations);
//            });
//        filters.getFilters().stream().filter(filter -> filter instanceof ExternalResourceHolder)
//            .forEach(filter -> {
//                final Set<String> locations =
//                    ((ExternalResourceHolder) filter).getExternalResourceLocations();
//                externalResources.addAll(locations);
//            });
        return externalResources;
    }

    /** Notify all listeners about the audit start. */
    private void fireAuditStarted() {
//        final AuditEvent event = new AuditEvent(this);
//        for (final AuditListener listener : listeners) {
//            listener.auditStarted(event);
//        }
    }

    /** Notify all listeners about the audit end. */
    private void fireAuditFinished() {
//        final AuditEvent event = new AuditEvent(this);
//        for (final AuditListener listener : listeners) {
//            listener.auditFinished(event);
//        }
    }

    /**
     * Processes a list of files with all FileSetChecks.
     * @param files a list of files to process.
     * @throws CheckstyleException if error condition within Checkstyle occurs.
     * @noinspection ProhibitedExceptionThrown
     */
    private void processFiles(List<File> files) throws CheckstyleException {
        for (final File file : files) {
            try {
                final String fileName = file.getAbsolutePath();
                final long timestamp = file.lastModified();
                if (cache != null && cache.isInCache(fileName, timestamp)
//                        || CommonUtils.matchesFileExtension(file, fileExtensions)
                        || !acceptFileStarted(fileName)) {
                    continue;
                }
                if (cache != null) {
                    cache.put(fileName, timestamp);
                }
                fireFileStarted(fileName);
                final SortedSet<String> fileMessages = processFile(file);
                fireErrors(fileName, fileMessages);
                fireFileFinished(fileName);
            }
            // -@cs[IllegalCatch] There is no other way to deliver filename that was under
            // processing. See https://github.com/checkstyle/checkstyle/issues/2285
            catch (Exception ex) {
                // We need to catch all exceptions to put a reason failure (file name) in exception
                throw new CheckstyleException("Exception was thrown while processing "
                        + file.getPath(), ex);
            }
            catch (Error error) {
                // We need to catch all errors to put a reason failure (file name) in error
                throw new Error("Error was thrown while processing " + file.getPath(), error);
            }
        }
    }

    /**
     * Processes a file with all FileSetChecks.
     * @param file a file to process.
     * @return a sorted set of messages to be logged.
     * @throws CheckstyleException if error condition within Checkstyle occurs.
     * @noinspection ProhibitedExceptionThrown
     */
    private SortedSet<String> processFile(File file) throws CheckstyleException {
        final SortedSet<String> fileMessages = new TreeSet<String>();
        try {
            final List<String> theText = new ArrayList<String>();
            for (final String fsc : fileSetChecks) {
            	theText.add(fsc);
            }
        }
        // -@cs[IllegalCatch] There is no other way to obey haltOnException field
        catch (Exception ex) {
            if (haltOnException) {
                throw ex;
            }

//            LOG.debug("Exception occurred.", ex);
            fileMessages.add("Exception occurred.");
        }
        return fileMessages;
    }

    /**
     * Check if all before execution file filters accept starting the file.
     *
     * @param fileName
     *            the file to be audited
     * @return {@code true} if the file is accepted.
     */
    private boolean acceptFileStarted(String fileName) {
		return true;
    }

    /**
     * Notify all listeners about the beginning of a file audit.
     *
     * @param fileName
     *            the file to be audited
     */
    public void fireFileStarted(String fileName) {
//        final AuditEvent event = new AuditEvent(this, stripped);
//        for (final AuditListener listener : listeners) {
//            listener.fileStarted(event);
//        }
    }

    /**
     * Notify all listeners about the errors in a file.
     *
     * @param fileName the audited file
     * @param errors the audit errors from the file
     */
    public void fireErrors(String fileName, SortedSet<String> errors) {
//        final String stripped = CommonUtils.relativizeAndNormalizePath(basedir, fileName);
//        boolean hasNonFilteredViolations = false;
//        for (final LocalizedMessage element : errors) {
//            final AuditEvent event = new AuditEvent(this, stripped, element);
//            if (filters.accept(event)) {
//                hasNonFilteredViolations = true;
//                for (final AuditListener listener : listeners) {
//                    listener.addError(event);
//                }
//            }
//        }
//        if (hasNonFilteredViolations && cache != null) {
//            cache.remove(fileName);
//        }
    }

    /**
     * Notify all listeners about the end of a file audit.
     *
     * @param fileName
     *            the audited file
     */
    public void fireFileFinished(String fileName) {
//        final String stripped = CommonUtils.relativizeAndNormalizePath(basedir, fileName);
//        final AuditEvent event = new AuditEvent(this, stripped);
//        for (final AuditListener listener : listeners) {
//            listener.fileFinished(event);
//        }
    }

    public void finishLocalSetup() throws CheckstyleException {
        @SuppressWarnings("unused")
		final Locale locale = new Locale(localeLanguage, localeCountry);
//        LocalizedMessage.setLocale(locale);

        if (moduleFactory == null) {

            if (moduleClassLoader == null) {
                throw new CheckstyleException(
                        "if no custom moduleFactory is set, "
                                + "moduleClassLoader must be specified");
            }

            @SuppressWarnings("unused")
			final Set<String> packageNames = PackageNamesLoader
                    .getPackageNames(moduleClassLoader);
//            moduleFactory = new PackageObjectFactory(packageNames,
//                    moduleClassLoader);
        }

        final List<Pair<String, Object>> context = new ArrayList<Pair<String, Object>>();
        context.add(new Pair<String, Object>("charset", charset));
        context.add(new Pair<String, Object>("classLoader", classLoader));
        context.add(new Pair<String, Object>("moduleFactory", moduleFactory));
//        context.add("severity", severityLevel.getName());
        context.add(new Pair<String, Object>("basedir", basedir));
//        childContext = context;
    }

    protected void setupChild(Configuration childConf)
            throws CheckstyleException {
        final String name = childConf.getName();
        @SuppressWarnings("unused")
		final Object child;

        try {
            child = moduleFactory.createModule(name);

//            if (child instanceof AutomaticBean) {
//                final AutomaticBean bean = (AutomaticBean) child;
//                bean.contextualize(childContext);
//                bean.configure(childConf);
//            }
        }
        catch (final CheckstyleException ex) {
            throw new CheckstyleException("cannot initialize module " + name
                    + " - " + ex.getMessage(), ex);
        }
//        if (child instanceof FileSetCheck) {
//            final FileSetCheck fsc = (FileSetCheck) child;
//            fsc.init();
//            addFileSetCheck(fsc);
//        }
//        else if (child instanceof BeforeExecutionFileFilter) {
//            final BeforeExecutionFileFilter filter = (BeforeExecutionFileFilter) child;
//            addBeforeExecutionFileFilter(filter);
//        }
//        else if (child instanceof Filter) {
//            final Filter filter = (Filter) child;
//            addFilter(filter);
//        }
//        else if (child instanceof AuditListener) {
//            final AuditListener listener = (AuditListener) child;
//            addListener(listener);
//        }
        
            throw new CheckstyleException(name
                    + " is not allowed as a child in Checker");
        
    }

    /**
     * Adds a FileSetCheck to the list of FileSetChecks
     * that is executed in process().
     * @param fileSetCheck the additional FileSetCheck
     */
    public void addFileSetCheck(String fileSetCheck) {
//        fileSetCheck.setMessageDispatcher(this);
        fileSetChecks.add(fileSetCheck);
    }

    /**
     * Adds a before execution file filter to the end of the event chain.
     * @param filter the additional filter
     */
    public void addBeforeExecutionFileFilter(String filter) {
//        beforeExecutionFileFilters.addBeforeExecutionFileFilter(filter);
    }

    /**
     * Adds a filter to the end of the audit event filter chain.
     * @param filter the additional filter
     */
    public void addFilter(String filter) {
//        filters.addFilter(filter);
    }

    public final void addListener(String listener) {
//        listeners.add(listener);
    }

    /**
     * Sets the file extensions that identify the files that pass the
     * filter of this FileSetCheck.
     * @param extensions the set of file extensions. A missing
     *     initial '.' character of an extension is automatically added.
     */
    public final void setFileExtensions(String... extensions) {
        if (extensions == null) {
            fileExtensions = null;
        }
        else {
        	fileExtensions = new String[extensions.length];
            for (int i = 0; i < extensions.length; i++) {
                final String extension = extensions[i];
                if (extension.charAt(0) == '.') {
                    fileExtensions[i] = extension;
                }
                else {
                    fileExtensions[i] = "." + extension;
                }
            }
        }
    }

    /**
     * Sets the factory for creating submodules.
     *
     * @param moduleFactory the factory for creating FileSetChecks
     */
    public void setModuleFactory(ModuleFactory moduleFactory) {
        this.moduleFactory = moduleFactory;
    }

    /**
     * Sets locale country.
     * @param localeCountry the country to report messages
     */
    public void setLocaleCountry(String localeCountry) {
        this.localeCountry = localeCountry;
    }

    /**
     * Sets locale language.
     * @param localeLanguage the language to report messages
     */
    public void setLocaleLanguage(String localeLanguage) {
        this.localeLanguage = localeLanguage;
    }

    /**
     * Sets the severity level.  The string should be one of the names
     * defined in the {@code SeverityLevel} class.
     *
     * @param severity  The new severity level
     * @see SeverityLevel
     */
    public final void setSeverity(String severity) {
//        severityLevel = SeverityLevel.getInstance(severity);
    }

    /**
     * Sets the classloader that is used to contextualize fileset checks.
     * Some Check implementations will use that classloader to improve the
     * quality of their reports, e.g. to load a class and then analyze it via
     * reflection.
     * @param classLoader the new classloader
     */
    public final void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Sets the classloader that is used to contextualize fileset checks.
     * Some Check implementations will use that classloader to improve the
     * quality of their reports, e.g. to load a class and then analyze it via
     * reflection.
     * @param loader the new classloader
     * @deprecated use {@link #setClassLoader(ClassLoader loader)} instead.
     */
    @Deprecated
    public final void setClassloader(ClassLoader loader) {
        classLoader = loader;
    }

    public final void setModuleClassLoader(ClassLoader moduleClassLoader) {
        this.moduleClassLoader = moduleClassLoader;
    }

    /**
     * Sets a named charset.
     * @param charset the name of a charset
     * @throws UnsupportedEncodingException if charset is unsupported.
     */
    public void setCharset(String charset)
            throws UnsupportedEncodingException {
        if (!Charset.isSupported(charset)) {
            final String message = "unsupported charset: '" + charset + "'";
            throw new UnsupportedEncodingException(message);
        }
        this.charset = charset;
    }

    /**
     * Sets the field haltOnException.
     * @param haltOnException the new value.
     */
    public void setHaltOnException(boolean haltOnException) {
        this.haltOnException = haltOnException;
    }

    /**
     * Clears the cache.
     */
    public void clearCache() {
        if (cache != null) {
            cache.reset();
        }
    }
    
   	public static void main(String[] args) {
   		
 
   		CheckerNullTest checker = new CheckerNullTest();
		ArrayList <File> files = new ArrayList<File>();
		SortedSet<String> errors = null;
		
		try {
			checker.process(files);
			checker.fireErrors("name", errors);
		} catch (CheckstyleException e) {
		}
		
		checker.clearCache();
		checker.destroy();		
		try {
			checker.setCacheFile("fileName");
		} catch (IOException e) {
		}
   	}
}
