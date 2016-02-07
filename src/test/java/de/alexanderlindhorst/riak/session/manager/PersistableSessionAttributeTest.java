/*
 * This software is licensed under the GPL v2 (http://www.gnu.org/licenses/gpl-2.0.html).
 */
package de.alexanderlindhorst.riak.session.manager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author lindhrst
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistableSessionAttributeTest {

    @Test
    public void attributeKeyCanBeRead() {
        PersistableSessionAttribute attribute = new PersistableSessionAttribute("key", "value");
        assertThat(attribute.getKey(), is("key"));
    }

    @Test
    public void attributeValueCanBeRead() {
        PersistableSessionAttribute attribute = new PersistableSessionAttribute("key", "value");
        assertThat(attribute.getValue(), is("value"));
    }

    @Test
    public void twoInstancesWithSameConfigGiveSameHashCode() {
        PersistableSessionAttribute attribute1 = new PersistableSessionAttribute("key", "value");
        PersistableSessionAttribute attribute2 = new PersistableSessionAttribute("key", "value");

        assertThat(attribute1.hashCode(), is(attribute2.hashCode()));
    }

    @Test
    public void twoInstancesWithSameConfigAreConsideredEqual() {
        PersistableSessionAttribute attribute1 = new PersistableSessionAttribute("key", "value");
        PersistableSessionAttribute attribute2 = new PersistableSessionAttribute("key", "value");

        assertThat(attribute1.equals(attribute2), is(true));
    }

    @Test
    public void sameInstanceWillBeConsideredEqual() {
        PersistableSessionAttribute attribute = new PersistableSessionAttribute("key", "value");
        PersistableSessionAttribute copy = attribute;
        assertThat(attribute.equals(copy), is(true));
    }

    @Test
    public void nullObjectsAreConsideredNotEqual() {
        PersistableSessionAttribute attribute = new PersistableSessionAttribute("key", "value");
        assertThat(attribute.equals(null), is(false));
    }

    @Test
    public void differentClassWillBeConsideredNotEqual() {
        PersistableSessionAttribute attribute = new PersistableSessionAttribute("key", "value");
        String string = "string";
        assertThat(attribute.equals(string), is(false));
    }

    @Test
    public void differentKeysAreConsideredNotEqual() {
        PersistableSessionAttribute attribute1 = new PersistableSessionAttribute("key1", "value");
        PersistableSessionAttribute attribute2 = new PersistableSessionAttribute("key2", "value");

        assertThat(attribute1.equals(attribute2), is(false));
    }

    @Test
    public void differentValuesAreConsideredNotEqual() {
        PersistableSessionAttribute attribute1 = new PersistableSessionAttribute("key", "value1");
        PersistableSessionAttribute attribute2 = new PersistableSessionAttribute("key", "value2");

        assertThat(attribute1.equals(attribute2), is(false));
    }
}
