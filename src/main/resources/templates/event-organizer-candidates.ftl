<#if candidates?size == 0>
<option value="" disabled selected>候補となるメンバーがいません</option>
<#else>
<option value="" disabled selected>選択してください</option>
<#list candidates as m>
<option value="${m.id?c}">${m.name}</option>
</#list>
</#if>
