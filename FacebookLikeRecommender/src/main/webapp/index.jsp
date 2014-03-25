<html>
<body>
<h2>Welcome to my simple demo recommendation system. The implementation is based on Apache Mahout's user based CF algorithm. The input set of likes is denoted by a new, anonymous user, and the recommendation system tries to recommend likes or users for this user.</h2>
<h3> Recommend likes based on input set of likes</h3>
Example queries: <br>
<a href="RecommenderServlet?recommend=likes&likes=Disney,YouTube,Facebook,Shakira,Lady Gaga">RecommenderServlet?recommend=likes&likes=Disney,YouTube,Facebook,Shakira,Lady Gaga</a>
<br><a href="RecommenderServlet?recommend=likes&likes=Basketball">RecommenderServlet?recommend=likes&likes=Basketball</a>
<br><a href="RecommenderServlet?recommend=likes&likes=Kanye%20West,Dr.%20Dre">RecommenderServlet?recommend=likes&likes=Kanye%20West,Dr.%20Dre</a>
<br>
<br>
<h3> Recommend users based on input set of likes</h3>
The output is a list of users with their likes.
Example queries: <br>
<a href="RecommenderServlet?recommend=users&likes=Disney,YouTube,Facebook,Shakira,Lady Gaga">RecommenderServlet?recommend=users&likes=Disney,YouTube,Facebook,Shakira,Lady Gaga</a>
<br><a href="RecommenderServlet?recommend=users&likes=Basketball">RecommenderServlet?recommend=users&likes=Basketball</a>
<br><a href="RecommenderServlet?recommend=users&likes=Kanye%20West,Dr.%20Dre">RecommenderServlet?recommend=users&likes=Kanye%20West,Dr.%20Dre</a>

<br>
<br>
<h3> Recommend users based on input set of users</h3>
The output is a list of users with their likes.
Example query: <br>
<br><a href="RecommenderServlet?recommend=users&users=ccf8bf64c7ff7f781ab27cbdd0b80cd2">RecommenderServlet?recommend=users&users=ccf8bf64c7ff7f781ab27cbdd0b80cd2.</a>> (This user likes basketball and LA Lakers)

</body>
</html>
