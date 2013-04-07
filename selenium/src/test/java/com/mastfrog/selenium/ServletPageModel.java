package com.mastfrog.selenium;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

/**
 *
 * @author Tim Boudreau
 */
public class ServletPageModel {

    @FindBy(how = How.ID, using = "searchField")
    @CacheLookup
    public WebElement searchField;
    @FindBy(how = How.ID, using = "searchSubmit")
    public WebElement searchButton;
    @FindBy(how = How.ID, using = "prev")
    public WebElement prev;
}
