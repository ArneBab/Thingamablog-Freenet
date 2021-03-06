
package net.sf.thingamablog.gui.properties;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import net.atlanticbb.tantlinger.ui.UIUtils;
import net.atlanticbb.tantlinger.ui.text.TextEditPopupManager;
import net.sf.thingamablog.blog.Author;
import net.sf.thingamablog.blog.BackendException;
import net.sf.thingamablog.blog.TBWeblog;
import net.sf.thingamablog.blog.TemplatePack;
import net.sf.thingamablog.blog.WeblogBackend;
import net.sf.thingamablog.blog.WeblogsDotComPing;
import net.sf.thingamablog.gui.LabelledItemPanel;
import net.sf.thingamablog.gui.MultilineText;
import net.sf.thingamablog.gui.app.TemplateSelectionPanel;
import net.sf.thingamablog.gui.app.WeblogPreviewer;
import net.sf.thingamablog.transport.FCPTransport;
import net.sf.thingamablog.transport.LocalTransport;
import net.sf.thingamablog.util.freenet.fcp.fcpManager;
import thingamablog.l10n.i18n;




/**
 * @author Dieppe
 *
 *
 *
 */
public class TBFlogWizardDialog extends JDialog {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    private Logger logger = Logger.getLogger("net.sf.thingamablog.gui.properties");
    
    private static final String CANCEL = i18n.str("cancel"); //$NON-NLS-1$
    private static final String FINISH = i18n.str("finish");	 //$NON-NLS-1$
    
    private CardLayout wizLayout;
    private JPanel wizPanel;
    
    private PropertyPanel starterPanel;
    private TitleDescrPanel titlePanel;
    private CategoriesPanel catPanel;
    private AuthorsPanel authPanel;
    private PropertyPanel emailPanel;
    private PropertyPanel templPanel;
    private PropertyPanel transportPanel;
    private PropertyPanel donePanel;
    private Vector panels = new Vector();
    
    private JButton nextButton, backButton, doneButton;
    
    private boolean isCancelled;
    
    private TBWeblog flog;
    
    private TextEditPopupManager popupManager = TextEditPopupManager.getInstance();
    
    private TemplatePack selectedPack;
    
    public TBFlogWizardDialog(Frame f, File dir, WeblogBackend backend, TBWeblog flog) {
        super(f, true);
        setTitle(i18n.str("new_flog")); //$NON-NLS-1$
        this.flog = flog;
        WindowAdapter windowAdapter = new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                cancelDialog();
            }
        };
        addWindowListener(windowAdapter);        
        
        //weblog.setAuthorStore(authStore);
        //weblog.setCategoryStore(catStore);
        
        wizLayout = new CardLayout();
        wizPanel = new JPanel(wizLayout);
        
        starterPanel = new StarterPanel();
        starterPanel.setBorder(new EmptyBorder(15, 10, 15, 10));
        panels.add(starterPanel);
        
        titlePanel = new TitleDescrPanel();
        titlePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panels.add(titlePanel);
        
        catPanel = new CategoriesPanel();
        catPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panels.add(catPanel);
        
        authPanel = new AuthorsPanel();
        authPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panels.add(authPanel);
        
        emailPanel = new EmailPanel();
        emailPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panels.add(emailPanel);
        
        templPanel = new TemplatePanel();
        templPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panels.add(templPanel);
        
        transportPanel = new TransportPanel();
        transportPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panels.add(transportPanel);
        
        donePanel = new DonePanel();
        donePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panels.add(donePanel);
        
        wizPanel.add(starterPanel, "1"); //$NON-NLS-1$
        wizPanel.add(titlePanel, "2"); //$NON-NLS-1$
        wizPanel.add(catPanel, "3"); //$NON-NLS-1$
        wizPanel.add(authPanel, "4"); //$NON-NLS-1$
        wizPanel.add(emailPanel, "5"); //$NON-NLS-1$
        wizPanel.add(templPanel, "6"); //$NON-NLS-1$
        wizPanel.add(transportPanel, "7"); //$NON-NLS-1$
        wizPanel.add(donePanel, "8");	 //$NON-NLS-1$
        
        ActionListener listener = new ButtonHandler();
        nextButton = new JButton(i18n.str("next-")); //$NON-NLS-1$
        nextButton.addActionListener(listener);
        backButton = new JButton(i18n.str("-back")); //$NON-NLS-1$
        backButton.setEnabled(false);
        backButton.addActionListener(listener);
        doneButton = new JButton(CANCEL);
        doneButton.addActionListener(listener);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setBorder(new EtchedBorder());
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(doneButton);
        controlPanel.add(buttonPanel);
        
        JLabel img = new JLabel();
        img.setVerticalAlignment(SwingConstants.TOP);
        img.setOpaque(true);
        img.setBackground(Color.WHITE);
        img.setIcon(UIUtils.getIcon(UIUtils.MISC, "wizard.jpg")); //$NON-NLS-1$
        
        getContentPane().add(wizPanel, BorderLayout.CENTER);
        getContentPane().add(controlPanel, BorderLayout.SOUTH);
        getContentPane().add(img, BorderLayout.WEST);
        
        pack();
        setSize(560, getHeight());
        setResizable(false);
    }
    
    public TBWeblog getWeblog() {
        return flog;
    }
    
    public boolean hasUserCancelled() {
        return isCancelled;
    }
    
    private void doFinish() {
                /*for(int i = 0; i < panels.size(); i++)
                {
                        PropertyPanel p = (PropertyPanel)panels.elementAt(i);
                        p.saveProperties();
                }*/
        
        try {
            donePanel.saveProperties();
            selectedPack.installPack(flog.getHomeDirectory());
        } catch(Exception ex) {
            UIUtils.showError(this, ex);
        }
        
        //add a couple ping services
        WeblogsDotComPing ping = new WeblogsDotComPing();
        
        //removed because the TAMB ping server has been shutdown...
                /*ping.setServiceName("Updated Thingamablogs");
                ping.setServiceUrl("http://thingamablog.sourceforge.net/rpc.php");
                ping.setEnabled(true);
                weblog.addPingService(ping);*/
        
        ping = new WeblogsDotComPing();
        ping.setServiceName("weblogs.com"); //$NON-NLS-1$
        ping.setServiceUrl("http://rpc.weblogs.com/RPC2"); //$NON-NLS-1$
        ping.setEnabled(false);
        flog.addPingService(ping);
        
        dispose();
    }
    
    private void cancelDialog() {
        isCancelled = true;
        try{
            flog.deleteAll();
        }catch(BackendException ex){}
        dispose();
    }
    
    public void dispose() {
        WeblogPreviewer.getInstance().clearPreviewData();
        super.dispose();
    }
    
    
    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel("<html><h2>" + text + "</h2></html>"); //$NON-NLS-1$ //$NON-NLS-2$
        return label;
    }
    
    
    
    private PropertyPanel getCurrentPanel() {
        for(int i = 0; i < panels.size(); i++) {
            PropertyPanel p = (PropertyPanel)panels.elementAt(i);
            if(p.isVisible()) {
                //return p.isValidData();
                return p;
            }
        }
        
        return null;
    }
    
    private class ButtonHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if(e.getSource() == nextButton) {
                if(!donePanel.isVisible()) {
                    //if(isCurrentPanelValid())
                    PropertyPanel p = getCurrentPanel();
                    if(p != null && p.isValidData()) {
                        p.saveProperties();
                        wizLayout.next(wizPanel);
                    }
                }
                
                if( donePanel.isVisible()) {
                    doneButton.setText(FINISH);
                    nextButton.setEnabled(false);
                }
                backButton.setEnabled(true);
            } else if(e.getSource() == backButton) {
                if(!starterPanel.isVisible())
                    wizLayout.previous(wizPanel);
                if(starterPanel.isVisible())
                    backButton.setEnabled(false);
                if(doneButton.getText().equals(FINISH))
                    doneButton.setText(CANCEL);
                nextButton.setEnabled(true);
            } else if(e.getSource() == doneButton) {
                //the new Weblog was canceled, so delete the
                //directory structure that was created when
                //the Weblog was instantiated
                if(doneButton.getText().equals(FINISH)) {
                    doFinish();
                } else {
                    cancelDialog();
                }
            }
        }
    }
    
    
    
    
    private class StarterPanel extends PropertyPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private JTextField pathField = new JTextField(20);
        private JTextField requestUriField = new JTextField(20);
        private JTextField insertUriField = new JTextField(20);
        private fcpManager Manager = new fcpManager();
        
        public StarterPanel() {
            LabelledItemPanel lip = new LabelledItemPanel();
            JLabel header;
            String text;
            if(flog.getPublishTransport() instanceof LocalTransport) {
                header = createHeaderLabel(i18n.str("flog_wizard_local"));			 //$NON-NLS-1$
                text = i18n.str("welcome_flog_panel_text_local"); //$NON-NLS-1$
                pathField.setText("The path of the flog's export directory");
                requestUriField.setText("The entire request uri of your flog");
                lip.addItem(i18n.str("base_path"), pathField); //$NON-NLS-1$
                lip.addItem(i18n.str("requestUri"), requestUriField); //$NON-NLS-1$
            } else {
                header = createHeaderLabel(i18n.str("flog_wizard_fcp"));
                text = i18n.str("welcome_flog_panel_text_fcp"); //$NON-NLS-1$
                requestUriField.setEditable(false);
                insertUriField.setEditable(false);
                pathField.setText("none");
                requestUriField.setText(flog.getBaseUrl());
                insertUriField.setText("USK@" + ((FCPTransport) flog.getPublishTransport()).getInsertURI() + "/");
                lip.addItem(i18n.str("requestUri"), requestUriField); //$NON-NLS-1$
                lip.addItem(i18n.str("insertUri"), insertUriField); //$NON-NLS-1$
            }            
            
            popupManager.registerJTextComponent(pathField);
            popupManager.registerJTextComponent(requestUriField);
            
            setLayout(new BorderLayout());
            add(header, BorderLayout.NORTH);
            add(new MultilineText(text), BorderLayout.CENTER);
            add(lip, BorderLayout.SOUTH);
        }
        
        public boolean isValidData() {
            if(pathField.getText().equals("")) //$NON-NLS-1$
            {
                JOptionPane.showMessageDialog(TBFlogWizardDialog.this,
                        i18n.str("invalid_path_prompt"), i18n.str("invalid_path"),  //$NON-NLS-1$ //$NON-NLS-2$
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
            
            if(requestUriField.getText().equals("")) //$NON-NLS-1$
            {
                JOptionPane.showMessageDialog(TBFlogWizardDialog.this,
                        i18n.str("invalid_url_prompt"), i18n.str("invalid_url"),  //$NON-NLS-1$ //$NON-NLS-2$
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
            boolean valid = true;
            valid = valid && isValidSSK(requestUriField.getText());
            valid = valid && isValidSSK(insertUriField.getText());
            return valid;
        }
        private boolean isValidSSK(String u) {
            // TODO : Check if u match a SSK key
            return true;
        }
        public void saveProperties() {
            String path = pathField.getText();
            String url = requestUriField.getText();
            if(!url.endsWith("/")){ //$NON-NLS-1$
                url += "/";; //$NON-NLS-1$
            }
            // If the flog is publish localy, we need a slash before the key
            if(!url.startsWith("/") && flog.getPublishTransport() instanceof LocalTransport){
                url = "/" + url;
            }
            String arcUrl = url + "archives"; //$NON-NLS-1$
            String mediaUrl = url + "medias"; //$NON-NLS-1$
            
            flog.setBlogUrls(path, url, arcUrl, mediaUrl);
            flog.setType("freenet");
        }
    }
    
    private class TitleDescrPanel extends PropertyPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private JTextField titleField = new JTextField();
        private JTextArea textArea = new JTextArea(4, 4);
        
        public TitleDescrPanel() {
            setLayout(new BorderLayout());
            
            JPanel instrPanel = new JPanel(new BorderLayout());
            JLabel header = createHeaderLabel(i18n.str("title_and_description")); //$NON-NLS-1$
            String text =
                    i18n.str("title_panel_text"); //$NON-NLS-1$
            instrPanel.add(header, BorderLayout.NORTH);
            instrPanel.add(new MultilineText(text), BorderLayout.CENTER);
            
            popupManager.registerJTextComponent(titleField);
            popupManager.registerJTextComponent(textArea);
            
            LabelledItemPanel lip = new LabelledItemPanel();
            lip.addItem(i18n.str("site_title"), titleField); //$NON-NLS-1$
            lip.addItem(i18n.str("description"), new JScrollPane(textArea)); //$NON-NLS-1$
            
            add(instrPanel, BorderLayout.NORTH);
            add(lip, BorderLayout.CENTER);
        }
        
        public boolean isValidData() {
            if(titleField.getText().equals("")) //$NON-NLS-1$
            {
                JOptionPane.showMessageDialog(TBFlogWizardDialog.this,
                        i18n.str("invalid_title_prompt"), i18n.str("title"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }
            return true;
        }
        
        public void saveProperties() {
            flog.setTitle(titleField.getText());
            flog.setDescription(textArea.getText());
        }
        
        public String getTitle() {
            return titleField.getText();
        }
        
        public String getDescription() {
            return textArea.getText();
        }
    }
    
    private class CategoriesPanel extends PropertyPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private EditableList list;
        private WeblogEditableListModel model;
        
        public CategoriesPanel() {
            setLayout(new BorderLayout(5, 5));
            
            JPanel instrPanel = new JPanel(new BorderLayout());
            JLabel header = createHeaderLabel(i18n.str("categories")); //$NON-NLS-1$
            String text =
                    i18n.str("categories_panel_text"); //$NON-NLS-1$
            instrPanel.add(header, BorderLayout.NORTH);
            instrPanel.add(new MultilineText(text), BorderLayout.CENTER);
            
            model = new WeblogEditableListModel(WeblogEditableListModel.CATEGORIES);
            list = new EditableList(model);
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            
            add(instrPanel, BorderLayout.NORTH);
            add(list, BorderLayout.CENTER);
            
            
        }
        
        public boolean isValidData() {
            return true;
        }
        
        public void saveProperties() {
            //ListModel lm = catList.getModel();
            try {
                model.syncListWithWeblog(flog);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        
        public String[] getCategories() {
            List data = list.getListData();
            String[] cats = new String[data.size()];
            for(int i = 0; i < cats.length; i++)
                cats[i] = data.get(i).toString();
            return cats;
        }
        
        public WeblogEditableListModel getModel() {
            return model;
        }
    }
    
    private class AuthorsPanel extends PropertyPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private EditableList list;
        private WeblogEditableListModel model;
        
        public AuthorsPanel() {
            setLayout(new BorderLayout(5, 5));
            
            JPanel instrPanel = new JPanel(new BorderLayout());
            JLabel header = createHeaderLabel(i18n.str("authors")); //$NON-NLS-1$
            String text =
                    i18n.str("authors_panel_text"); //$NON-NLS-1$
            instrPanel.add(header, BorderLayout.NORTH);
            instrPanel.add(new MultilineText(text), BorderLayout.CENTER);
            
            model = new WeblogEditableListModel(WeblogEditableListModel.AUTHORS);
            list = new EditableList(model);
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            
            add(instrPanel, BorderLayout.NORTH);
            add(list, BorderLayout.CENTER);
        }
        
        public boolean isValidData() {
            return true;
        }
        
        public void saveProperties() {
            try {
                model.syncListWithWeblog(flog);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        
        public Author[] getAuthors() {
            List data = list.getListData();
            Author[] a = new Author[data.size()];
            for(int i = 0; i < a.length; i++)
                a[i] = (Author)data.get(i);
            return a;
        }
        
        public WeblogEditableListModel getModel() {
            return model;
        }
    }
    
    private class EmailPanel extends PropertyPanel {
        
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        TBEmailPanel emailPanel;
        
        public EmailPanel() {
            setLayout(new BorderLayout(5, 5));
            
            JPanel instrPanel = new JPanel(new BorderLayout());
            JLabel header = createHeaderLabel("Email"); //$NON-NLS-1$
            String text =
                    i18n.str("specify_mail_server_prompt"); //$NON-NLS-1$
            
            instrPanel.add(header, BorderLayout.NORTH);
            instrPanel.add(new MultilineText(text), BorderLayout.CENTER);
            
            emailPanel = new TBEmailPanel(flog);
            add(instrPanel, BorderLayout.NORTH);
            add(emailPanel, BorderLayout.CENTER);
        }
        
        /* (non-Javadoc)
         * @see net.sf.thingamablog.gui.properties.PropertyPanel#isValidData()
         */
        public boolean isValidData() {
            return emailPanel.isValidData();
        }
        
        /* (non-Javadoc)
         * @see net.sf.thingamablog.gui.properties.PropertyPanel#saveProperties()
         */
        public void saveProperties() {
            emailPanel.saveProperties();
        }
        
    }
    
    private class TransportPanel extends PropertyPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        TBPublishTransportPanel pubPanel;
        
        public TransportPanel() {
            setLayout(new BorderLayout());
            
            JPanel instrPanel = new JPanel(new BorderLayout());
            JLabel header = createHeaderLabel(i18n.str("publishing")); //$NON-NLS-1$
            String text =
                    i18n.str("publishing_panel_text"); //$NON-NLS-1$
            instrPanel.add(header, BorderLayout.NORTH);
            instrPanel.add(new MultilineText(text), BorderLayout.CENTER);
            
            pubPanel = new TBPublishTransportPanel(flog);
            add(instrPanel, BorderLayout.NORTH);
            add(pubPanel, BorderLayout.CENTER);
        }
        
        public boolean isValidData() {
            return pubPanel.isValidData();
        }
        
        public void saveProperties() {
            pubPanel.saveProperties();
        }
    }
    
    private class TemplatePanel extends PropertyPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        //private JComboBox tmplCombo;
        //private TemplatePropertiesPanel propertyPanel;
        private TemplateSelectionPanel selPanel;
        
        public TemplatePanel() {
            JPanel instrPanel = new JPanel(new BorderLayout());
            JLabel header = createHeaderLabel(i18n.str("templates")); //$NON-NLS-1$
            String text =
                    i18n.str("templates_panel_text"); //$NON-NLS-1$
            instrPanel.add(header, BorderLayout.NORTH);
            instrPanel.add(new MultilineText(text), BorderLayout.CENTER);
            
            selPanel = new TemplateSelectionPanel(flog);
            
            setLayout(new BorderLayout(5, 5));
            add(instrPanel, BorderLayout.NORTH);
            add(selPanel, BorderLayout.CENTER);
        }
        
        public boolean isValidData() {
            if(selPanel.getSelectedPack() == null)
                return false;
            return true;
        }
        
        public void saveProperties() {
            selectedPack = selPanel.getSelectedPack();
        }
    }
    
    private class DonePanel extends PropertyPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        
        public DonePanel() {
            JLabel header = createHeaderLabel(i18n.str("done")); //$NON-NLS-1$
            String text =
                    i18n.str("finished_panel_text"); //$NON-NLS-1$
            
            setLayout(new BorderLayout());
            add(header, BorderLayout.NORTH);
            add(new MultilineText(text), BorderLayout.CENTER);
        }
        
        public boolean isValidData() {
            return true;
        }
        
        public void saveProperties() {
            System.out.println("Creating the flog...");
        }
    }
}
