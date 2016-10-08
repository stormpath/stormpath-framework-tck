/*
 * Copyright 2016 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stormpath.tck.util

import com.jayway.restassured.path.xml.element.Node
import com.jayway.restassured.path.xml.element.NodeChildren

import static org.testng.Assert.assertEquals

class HtmlUtils {

    public static Node findTagWithAttribute(NodeChildren children, String tag, String attributeKey, String attributeValue) {
        for (Node node : children.list()) {
            def actualTag = node.name()
            def actualAttribute = node.attributes().get(attributeKey)

            if (actualTag == tag && actualAttribute.contains(attributeValue)) {
                return node
            }
            else {
                Node foundNode = findTagWithAttribute(node.children(), tag, attributeKey, attributeValue)
                if (foundNode != null) {
                    return foundNode
                }
            }
        }
    }

    public static List<Node> findTagsWithAttribute(NodeChildren children, String tag, String attributeKey, String attributeValue) {
        def results = new ArrayList<Node>()

        for (Node node in children.list()) {
            if (node.name() == tag && node.attributes().get(attributeKey).contains(attributeValue)) {
                results.add(node)
            }
            else {
                Collection<Node> innerResults =
                    findTagsWithAttribute(node.children(), tag, attributeKey, attributeValue)
                results.addAll(innerResults)
            }
        }

        return results
    }

    public static List<Node> findTags(NodeChildren children, String tag) {
        def results = new ArrayList<Node>()

        for (Node node in children.list()) {
            if (node.name() == tag) {
                results.add(node)
            }
            else {
                Collection<Node> innerResults = findTags(node.children(), tag)
                results.addAll(innerResults)
            }
        }

        return results
    }

    /**
     * Takes an array like:
     *         def requiredAttributesList = [
     [name: "givenName", placeholder: "First Name", type: "text"],
     [name: "surname", placeholder: "Last Name", type: "text"],
     [name: "email", placeholder: "Email", type: "email"],
     [name: "password", placeholder: "Password", type: "password"]
     ]

     And checks that the Nodes' elements equal those
     * @param htmlTags
     * @param requiredAttributesList
     */
    public static void assertAttributesEqual(List<Node> htmlTags, List<Map<String, String>> requiredAttributesList) {
        Iterator<Node> htmlTagsIterator = htmlTags.iterator()
        Iterator<LinkedHashMap<String, String>> requiredAttributesIterator = requiredAttributesList.iterator()

        while(htmlTagsIterator.hasNext() && requiredAttributesIterator.hasNext()) {
            Map<String, String> attributes = htmlTagsIterator.next().attributes()

            if(attributes.get("type", "").equalsIgnoreCase("hidden")) {
                continue
            }
            Map<String, String> requiredAttributes = requiredAttributesIterator.next()

            for(requiredAttribute in requiredAttributes) {
                String attribute = attributes[requiredAttribute.key]
                assertEquals(attribute, requiredAttribute.value)
            }
        }
    }
}
