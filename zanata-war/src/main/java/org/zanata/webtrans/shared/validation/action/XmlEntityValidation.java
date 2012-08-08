/*
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.webtrans.shared.validation.action;

import java.util.ArrayList;

import org.zanata.webtrans.client.resources.ValidationMessages;

import com.allen_sauer.gwt.log.client.Log;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

/**
 * 
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 * 
 **/
public class XmlEntityValidation extends AbstractValidation
{

   // &amp;, &quot;
   private final static String charRefRegex = "&[:a-z_A-Z][a-z_A-Z0-9.-]*;";
   private final static RegExp charRefExp = RegExp.compile(charRefRegex);

   // &#[numeric]
   private final static String decimalRefRegex = "&#[0-9]+;";
   private final static RegExp decimalRefExp = RegExp.compile(decimalRefRegex);

   // &#x[hexadecimal]
   private final static String hexadecimalRefRegex = "&#x[0-9a-f_A-F]+;";
   private final static RegExp hexadecimalRefExp = RegExp.compile(hexadecimalRefRegex);


   private final static RegExp charRefGlobalExp = RegExp.compile(charRefRegex, "g");
   private final static RegExp decimalRefGlobalExp = RegExp.compile(decimalRefRegex, "g");
   private final static RegExp hexadecimalRefGlobalExp = RegExp.compile(hexadecimalRefRegex, "g");

   private final static String ENTITY_START_CHAR = "&";

   // XML PREDEFINED ENTITY
   // private final static String[] PRE_DEFINED_ENTITY = { "&quot;", "&amp;",
   // "&apos;", "&lt;", "&gt;" };

   public XmlEntityValidation(final ValidationMessages messages)
   {
      super(messages.xmlEntityValidatorName(), messages.xmlEntityValidatorDescription(), true, messages);
   }

   @Override
   public void doValidate(String source, String target)
   {
      validateIncompleteEntity(target);
      validateSourceTargetEntity(source, target);
   }

   private void validateSourceTargetEntity(String source, String target)
   {
      if (Strings.isNullOrEmpty(source) || Strings.isNullOrEmpty(target))
      {
         return;
      }

      ArrayList<String> unmatched = new ArrayList<String>();
      unmatched.addAll(validate(source, target, charRefGlobalExp));
      unmatched.addAll(validate(source, target, decimalRefGlobalExp));
      unmatched.addAll(validate(source, target, hexadecimalRefGlobalExp));


      if (!unmatched.isEmpty())
      {
         addError(getMessages().entityMissing(unmatched));
      }

   }

   private ArrayList<String> validate(String source, String target, RegExp regExp)
   {
      ArrayList<String> unmatched = new ArrayList<String>();

      String tmp = target;
      MatchResult result = regExp.exec(source);

      while (result != null)
      {
         String entity = result.getGroup(0);
         Log.debug("Found entity:" + entity);
         if (!tmp.contains(entity))
         {
            unmatched.add(" [" + entity + "] ");
         }
         else
         {
            tmp = tmp.replaceFirst(entity, ""); // remove matched entity from
         }
         result = regExp.exec(source);
      }
      return unmatched;
   }

   private void validateIncompleteEntity(String target)
   {
      if (Strings.isNullOrEmpty(target))
      {
         return;
      }

      Iterable<String> words = Splitter.on(" ").trimResults().omitEmptyStrings().split(target);

      for (String word : words)
      {
         if (word.startsWith(ENTITY_START_CHAR) && word.length() > 1)
         {
            if (!charRefExp.test(word) && !decimalRefExp.test(word) && !hexadecimalRefExp.test(word))
            {
               addError(getMessages().invalidXMLEntity(word));
            }
         }
      }
   }
}
