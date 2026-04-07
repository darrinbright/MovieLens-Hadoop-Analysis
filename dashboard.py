import streamlit as st
import pandas as pd
import plotly.express as px
import os

st.set_page_config(page_title="MovieLens Big Data Analysis", layout="wide")

st.markdown("""
    <style>
    .main {
        background-color: #ffffff;
    }
    .stMetric {
        background-color: #f8f9fa;
        padding: 20px;
        border-radius: 5px;
        border: 1px solid #dee2e6;
        color: #212529;
    }
    h1, h2, h3 {
        color: #212529;
    }
    [data-testid="stMetricValue"] {
        color: #007bff;
    }
    </style>
    """, unsafe_allow_html=True)

st.title("MovieLens Big Data Analysis Dashboard")

def load_tsv(file_path, columns):
    if os.path.exists(file_path):
        return pd.read_csv(file_path, sep='\t', names=columns, skiprows=1)
    return pd.DataFrame(columns=columns)

movies_df = pd.read_csv('../movie.csv')

job1_df = load_tsv('results/job1_final.tsv', ['movieId', 'stats'])
if not job1_df.empty:
    job1_df[['rating_count', 'avg_rating']] = job1_df['stats'].str.split(',', expand=True)
    job1_df['rating_count'] = pd.to_numeric(job1_df['rating_count'])
    job1_df['avg_rating'] = pd.to_numeric(job1_df['avg_rating'])
    job1_display = job1_df.merge(movies_df[['movieId', 'title']], on='movieId').sort_values('rating_count', ascending=False)
else:
    job1_display = pd.DataFrame(columns=['title', 'rating_count', 'avg_rating'])

job2_df = load_tsv('results/job2_final.tsv', ['Genre', 'Count']).sort_values('Count', ascending=False)

job3_df = load_tsv('results/job3_final.tsv', ['userId', 'stats'])
if not job3_df.empty:
    job3_df[['rating_count', 'avg_rating']] = job3_df['stats'].str.split(',', expand=True)
    job3_df['rating_count'] = pd.to_numeric(job3_df['rating_count'])

col1, col2, col3 = st.columns(3)
with col1:
    st.metric("Total Movies", f"{len(movies_df):,}")
with col2:
    st.metric("Total Ratings", "20,000,263")
with col3:
    st.metric("Hadoop Analytics Jobs", "6 Completed (Java)")

row1_col1, row1_col2 = st.columns([2, 1])

with row1_col1:
    st.subheader("Top 10 Most Rated Movies")
    st.dataframe(job1_display[['title', 'rating_count', 'avg_rating']].head(10), 
                 use_container_width=True, hide_index=True)

with row1_col2:
    st.subheader("Genre Distribution")
    if not job2_df.empty:
        fig_genres = px.pie(job2_df.head(10), values='Count', names='Genre', 
                           hole=0.3, color_discrete_sequence=px.colors.qualitative.Set3)
        fig_genres.update_layout(template="simple_white")
        st.plotly_chart(fig_genres, use_container_width=True)

st.subheader("User Activity Analysis")
if not job3_df.empty:
    fig_users = px.bar(job3_df.sort_values('rating_count', ascending=False).head(15), 
                      x='userId', y='rating_count', 
                      title="Most Active Users (Top 15)",
                      labels={'userId': 'User ID', 'rating_count': 'Total Ratings'},
                      template="simple_white")
    st.plotly_chart(fig_users, use_container_width=True)

st.divider()
st.subheader("Tag Alphabet Distribution")
job6_df = load_tsv('results/job6_final.tsv', ['Letter', 'Count']).sort_values('Letter')
if not job6_df.empty:
    fig_tags = px.line(job6_df, x='Letter', y='Count', markers=True, 
                      title="Tag count distribution by starting letter",
                      template="simple_white")
    st.plotly_chart(fig_tags, use_container_width=True)
